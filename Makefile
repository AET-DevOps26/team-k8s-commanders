# CareDesk — one-command deployment targets
#
# Pick where to deploy with DEPLOY_TARGET (local | aet). Default: aet.
#   make deploy                      -> deploy to DEPLOY_TARGET (default aet)
#   make deploy DEPLOY_TARGET=local  -> deploy to local kind cluster
#   make deploy DEPLOY_TARGET=aet    -> deploy to AET TUM Rancher cluster
# DEPLOY_TARGET can also be set inline (DEPLOY_TARGET=local make deploy)
# or as a line in .env.k8s.
#
# Local (kind) shortcuts:
#   make local         -> alias for `make deploy DEPLOY_TARGET=local`
#   make local-clean   -> delete kind cluster
#
# AET TUM cluster (uses .env.k8s):
#   make undeploy      -> uninstall release (keeps PVCs + namespace)
#   make purge         -> uninstall + delete PVCs + delete namespace
#
# Chart dev:
#   make lint          -> helm lint
#   make template      -> render manifests (no cluster needed)
#   make dry-run       -> helm dry-run against current cluster

SHELL         := /usr/bin/env bash
CHART         := helm/caredesk
TUM_ID       ?= ge38yuc
NS            := $(TUM_ID)-devops26
RELEASE      ?= caredesk
DEPLOY_TARGET ?= aet

.PHONY: local local-clean deploy undeploy purge lint template dry-run dep help

help:
	@grep -E '^(#|[a-zA-Z_-]+:)' Makefile | sed 's/^# //'

# ─── Deploy (local | aet, chosen by DEPLOY_TARGET) ────────────────────────────
deploy:
	@DEPLOY_TARGET=$(DEPLOY_TARGET) ./scripts/deploy.sh

# Local kind shortcut: same as `make deploy DEPLOY_TARGET=local`
local:
	@DEPLOY_TARGET=local ./scripts/deploy.sh

local-clean:
	@kind delete cluster --name caredesk

undeploy:
	@./scripts/undeploy-k8s.sh

purge:
	@./scripts/undeploy-k8s.sh --purge

# ─── Chart utilities ──────────────────────────────────────────────────────────
# No subchart dependencies — databases are plain postgres Deployments.
lint:
	@helm lint $(CHART)

template:
	@helm template $(RELEASE) $(CHART) --namespace $(NS) \
		--set ai.secrets.llmApiKey=dummy \
		--set backend.jwtSecret=dummy-dev-jwt-secret-min-32-characters \
		--set postgres.password=dummy

dry-run:
	@helm upgrade --install $(RELEASE) $(CHART) --namespace $(NS) --dry-run --debug
