# CareDesk Monitoring Helm Chart

Deploys the **CareDesk observability stack** — **Prometheus** and **Grafana** —
into its own namespace (`team-k8s-commanders-monitoring`), separate from the app
([helm/caredesk](../caredesk/), namespace `team-k8s-commanders`).

Why a dedicated namespace:

- **Isolation** — monitoring upgrades/rollbacks never touch the app, and vice versa.
- **Quota clarity** — the stack's 350m CPU / 768Mi memory budget is accounted
  separately from the app namespace's 6000m/8192Mi quota (both namespaces share
  the Rancher **project** quota).
- **Cleaner permissions and troubleshooting** — observability resources are not
  mixed in with the microservices.

## Deploy

```bash
helm upgrade --install caredesk-monitoring helm/caredesk-monitoring \
  --namespace team-k8s-commanders-monitoring --create-namespace
```

For production, override the Grafana admin password:
`--set grafana.adminPassword=<secret>`.

> **Rancher note:** the first deploy creates the namespace outside the team's
> Rancher project. Move it into the project afterwards (Rancher UI → Cluster →
> Projects/Namespaces) so it shares the project quota. CI
> (`.github/workflows/deploy-k8s-monitoring.yml`) does the same
> create-if-missing dance and tolerates the quota appearing later.

## What gets deployed

| Component | Image | Port | Storage |
|-----------|-------|------|---------|
| Prometheus | `prom/prometheus` | 9090 | 2Gi PVC (7d retention) |
| Grafana | `grafana/grafana` | 3000 | none — fully provisioned from ConfigMaps |

- **Grafana** is exposed at
  `https://caredesk-monitoring-team-k8s-commanders.student.k8s.aet.cit.tum.de/`
  (own host, root path — no sub-path config). Log in with
  `grafana.adminUser` / `grafana.adminPassword`.
- **Prometheus is not exposed** through the ingress (its API has no auth). A
  NetworkPolicy restricts it to Grafana; for ad-hoc access use
  `kubectl -n team-k8s-commanders-monitoring port-forward svc/caredesk-monitoring-prometheus 9090`.

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

## Single source of truth with docker-compose

Dashboards and alert rules are the same files the compose stack mounts:

- `dashboards/` → symlink to `infra/grafana/dashboards`
- `alerting/` → symlink to `infra/grafana/provisioning/alerting`

Edit them once in `infra/grafana`, and both the compose stack and this chart
pick them up (CI redeploys the chart on changes to `infra/grafana/**`).
Everything Grafana needs is provisioned this way, so it needs no persistent
volume — dashboards/alerts survive restarts by construction, and ad-hoc UI
edits are intentionally not persisted (`editable: false`).

## Tear down

```bash
helm uninstall caredesk-monitoring -n team-k8s-commanders-monitoring
```

Removes everything including the Prometheus PVC (metric history is wiped).
