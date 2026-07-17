# CareDesk Monitoring Helm Chart

Deploys the **CareDesk observability stack** — **Prometheus**, **Tempo** and
**Grafana** — into its own namespace (`team-k8s-commanders-monitoring`),
separate from the app ([helm/caredesk](../caredesk/), namespace
`team-k8s-commanders`).

Why a dedicated namespace:

- **Isolation** — monitoring upgrades/rollbacks never touch the app, and vice versa.
- **Quota clarity** — the stack's 450m CPU / 1024Mi memory budget is accounted
  separately from the app namespace's 6000m/8192Mi quota (both namespaces share
  the Rancher **project** quota). That's the full 500m/1024Mi Rancher
  reservation described below — zero headroom left on memory, so raising any
  component's limit means raising the reservation (or another's) to match.
- **Cleaner permissions and troubleshooting** — observability resources are not
  mixed in with the microservices.

## Deploy

The chart ships **no default Grafana admin password** — supply one at install
time (rendering fails otherwise):

```bash
helm upgrade --install caredesk-monitoring helm/caredesk-monitoring \
  --namespace team-k8s-commanders-monitoring --create-namespace \
  --set grafana.adminPassword="$(openssl rand -hex 16)"
```

CI injects it from the `GRAFANA_ADMIN_PASSWORD` GitHub secret (see
`.github/workflows/deploy-k8s-monitoring.yml`).

> **Rancher note:** CI (`.github/workflows/deploy-k8s-monitoring.yml`) creates
> the namespace via `scripts/ensure-k8s-namespace.sh`. With the
> `RANCHER_PROJECT_ID` variable set in the AET environment
> (`<cluster-id>:<project-id>`, both visible in the Rancher URL while the
> project is open), a fresh namespace lands directly in the team's Rancher
> project **and reserves its share of the project quota** (500m CPU / 1024Mi;
> the app namespace reserves the remaining 5500m / 7168Mi) via the
> `field.cattle.io/resourceQuota` annotation — so a full redeploy after a
> cluster wipe reproduces the split without manual steps. Rancher allocates
> project quota by reservation, not usage: a namespace joining without a
> reservation only gets what the others left over (possibly 0). Without
> `RANCHER_PROJECT_ID`, the namespace is created unassigned and must be moved
> into the project manually (Rancher UI → Cluster → Projects/Namespaces); the
> quota check skips gracefully until then.

## What gets deployed

| Component | Image | Port | Storage |
|-----------|-------|------|---------|
| Prometheus | `prom/prometheus` | 9090 | 2Gi PVC (7d retention) |
| Tempo | `grafana/tempo` | 3200 (query), 4318 (OTLP/HTTP ingest) | 1Gi PVC (48h retention) |
| Grafana | `grafana/grafana` | 3000 | none — fully provisioned from ConfigMaps |

- **Grafana** is exposed at
  `https://caredesk-monitoring-team-k8s-commanders.student.k8s.aet.cit.tum.de/`
  (own host, root path — no sub-path config). Log in with
  `grafana.adminUser` / `grafana.adminPassword`.
- **Prometheus and Tempo are not exposed** through the ingress (neither API has
  auth). NetworkPolicies restrict each: Prometheus to Grafana only; Tempo to
  Grafana on 3200 (querying) plus the whole app namespace on 4318 (every
  service sends spans, not just one caller — see
  `templates/networkpolicy.yaml`). For ad-hoc access:
  `kubectl -n team-k8s-commanders-monitoring port-forward svc/caredesk-monitoring-prometheus 9090`
  (or `svc/caredesk-monitoring-tempo 3200`).

## Cross-namespace scraping

Prometheus reaches the app services by **fully-qualified Service DNS**
(`caredesk-<svc>.team-k8s-commanders.svc.cluster.local`) — static targets, no
Kubernetes RBAC needed. The caredesk chart's NetworkPolicies explicitly admit
pods labeled `app.kubernetes.io/component=prometheus` **from this namespace**
(bare podSelectors only match same-namespace pods). If scrape targets show as
`down` after a deploy, check that both charts agree on the namespace names
(`app.namespace` here vs. `monitoring.namespace` in the caredesk chart).

Scrape jobs are defined in `values.yaml` under `prometheus.targets`; the ports
mirror `<svc>.service.port` in the caredesk chart — keep them in sync.

## Cross-namespace tracing

Traffic runs the opposite direction from scraping: every CareDesk service
pushes spans **to** Tempo over OTLP/HTTP, rather than Tempo pulling from them.
Each service resolves Tempo's address itself
(`caredesk.otlpTracesEndpoint` in `helm/caredesk/templates/_helpers.tpl`,
built from `monitoring.release` + `monitoring.namespace` in the caredesk
chart's `values.yaml`) — if this chart's release name or namespace ever
changes, update those values too or every exporter silently fails to reach
Tempo (harmless — spans are just dropped — but traces stop appearing). Tempo's
own config (single-binary mode, local-disk storage) lives in
[`infra/tempo/tempo.yaml`](../../infra/tempo/tempo.yaml), symlinked into this
chart the same way as the Grafana dashboards/alert rules.

## Alert delivery (Discord + Mailpit email)

Alert rules and their delivery are provisioned from
`infra/grafana/provisioning/alerting/` (shared with compose): `alerts.yaml`
holds the rules, `contact-points.yaml` two contact points — Discord and email —
plus the notification policy that fans **every** alert out to both.

The email contact point relays into Mailpit (in the *app* namespace) so alerts
also land in the Mailpit inbox — useful for demos without Discord. It needs
Grafana's SMTP pointed at Mailpit, which `grafana.smtp` does by default
(`caredesk-mailpit.team-k8s-commanders.svc.cluster.local:1025`); if you rename
the app namespace or release, update `grafana.smtp.host` to match or alert
emails silently fail to send. The `caredesk-mailpit-email` contact point and
its route in the notification policy are unconditional — `--set
grafana.smtp.enabled=false` only stops the `GF_SMTP_*` env vars from being set
on the Grafana deployment, so email deliveries still get attempted and simply
fail (logged as errors) since SMTP isn't configured. There's no flag that
actually removes the email route; to do that, edit
`infra/grafana/provisioning/alerting/contact-points.yaml` directly.

The webhook URL is interpolated from the
`DISCORD_WEBHOOK_URL` env var at Grafana startup — set the `DISCORD_WEBHOOK_URL`
GitHub secret (CI passes it as `--set grafana.discordWebhookUrl=...`), or for
manual deploys pass the flag yourself. Unset, a non-resolving placeholder keeps
Grafana booting; alerts then only show in the UI.

To get the URL: Discord channel → Edit channel → Integrations → Webhooks →
New Webhook → Copy Webhook URL.

## Single source of truth with docker-compose

Dashboards, alert rules and Tempo's config are the same files the compose
stack mounts:

- `dashboards/` → symlink to `infra/grafana/dashboards`
- `alerting/` → symlink to `infra/grafana/provisioning/alerting`
- `tempo/` → symlink to `infra/tempo`

Edit them once in `infra/grafana` or `infra/tempo`, and both the compose stack
and this chart pick them up (CI redeploys the chart on changes to either
directory, see `.github/workflows/deploy-k8s-monitoring.yml`). Everything
Grafana needs is provisioned this way, so it needs no persistent volume —
dashboards/alerts survive restarts by construction, and ad-hoc UI edits are
intentionally not persisted (`editable: false`). Tempo still needs its own PVC
(trace data isn't config-as-code).

## Tear down

```bash
helm uninstall caredesk-monitoring -n team-k8s-commanders-monitoring
```

Removes everything including the Prometheus and Tempo PVCs (metric/trace
history is wiped).
