# CareDesk — one-command deployment targets
#
# Local (kind):
#   make local         -> build images, spin up kind, install ingress, deploy
#   make local-clean   -> delete kind cluster
#
# AET TUM cluster (uses .env.k8s):
#   make deploy        -> helm upgrade --install on Rancher AET cluster
#   make undeploy      -> uninstall release (keeps PVCs + namespace)
#   make purge         -> uninstall + delete PVCs + delete namespace
#
# Chart dev:
#   make lint          -> helm lint
#   make template      -> render manifests (no cluster needed)
#   make dry-run       -> helm dry-run against current cluster

SHELL    := /usr/bin/env bash
CHART    := helm/caredesk
TUM_ID  ?= ge38yuc
NS       := $(TUM_ID)-devops26
RELEASE ?= caredesk

.PHONY: local local-clean deploy undeploy purge lint template dry-run dep help

help:
	@grep -E '^(#|[a-zA-Z_-]+:)' Makefile | sed 's/^# //'

# ─── Local kind deploy ────────────────────────────────────────────────────────
local:
	@./scripts/deploy-local.sh

local-clean:
	@kind delete cluster --name caredesk

# ─── AET cluster deploy ───────────────────────────────────────────────────────
deploy:
	@./scripts/deploy-k8s.sh

undeploy:
	@./scripts/undeploy-k8s.sh

purge:
	@./scripts/undeploy-k8s.sh --purge

# ─── Chart utilities ──────────────────────────────────────────────────────────
dep:
	@helm repo add bitnami https://charts.bitnami.com/bitnami 2>/dev/null || true
	@helm dependency update $(CHART)

lint: dep
	@helm lint $(CHART)

template: dep
	@helm template $(RELEASE) $(CHART) --namespace $(NS) \
		--set ai.secrets.llmApiKey=dummy \
		--set auth.enabled=true \
		--set postgresql.enabled=true \
		--set auth.secrets.jwtSecret=dummy \
		--set postgresql.auth.password=dummy

dry-run: dep
	@helm upgrade --install $(RELEASE) $(CHART) --namespace $(NS) --dry-run --debug
