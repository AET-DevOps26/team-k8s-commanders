# team-k8s-commanders
Repository for team K8s Commanders

Local development setup
-----------------------

This project stores Git hooks and generator tooling in the repository so developers can get started with a single setup step.

Prerequisites
- Git
- Node.js and npm (recommended LTS)
- Python 3.10+ (for `openapi-python-client`)
- Java JDK (required by OpenAPI Generator)

Quick setup
1. From the repository root, run the consolidated setup script. It will:
	- install Node dev dependencies into `node_modules`,
	- create a Python virtualenv at `.venv` and install Python generator tools,
	- enable the repo-managed Git hooks, and
	- attempt an initial client generation.

```bash
./scripts/setup-all.sh
```

What the setup script does
- `scripts/setup-generators.sh`: installs Node dev dependencies from `package.json` and populates a local Python venv with `requirements-dev.txt`.
- `scripts/install-hooks.sh`: updates `core.hooksPath` to `git/hooks` so versioned shell hooks run automatically.
- `api/scripts/gen-all.sh`: runs the OpenAPI generator and client generators (used by the post-checkout/post-merge hooks).

Useful commands
- Re-run generator setup only (no hooks):

```bash
./scripts/setup-generators.sh
```

- Install hooks only:

```bash
./scripts/install-hooks.sh
```

- Generate clients manually:

```bash
./api/scripts/gen-all.sh
```

- Run the pre-commit OpenAPI lint hook manually:

```bash
./git/hooks/pre-commit
```

Where files are written
- Java server: `services/spring-order/generated/`
- Python client: `services/py-recommender/client/`
- TypeScript API: `web-client/src/api.ts`

These paths are ignored by `.gitignore` so generated output is not committed by accident.

Troubleshooting
- If generation fails because a tool is missing, check that `npm` and `python3` are installed and re-run `./scripts/setup-generators.sh`.
- The OpenAPI Generator requires a Java JDK on PATH. Install one if you see a Java-related error.
- If you prefer containerized generation (no local installs), I can add a `Makefile` + Docker target.

Updating generator tools
- To update Node tools: `npm install` or modify `package.json` and run `npm install`.
- To update Python tools: activate the venv and `pip install -r requirements-dev.txt`.

Notes
- Hooks are implemented as shell scripts under `git/hooks` and are authoritative; no `pre-commit` YAML is required. The consolidated setup script makes the repo ready for development in one step.

If you want, I can add `Makefile` targets (`make setup`, `make generate`) or a Docker target for reproducible generation. Tell me which you prefer and I’ll add it.
