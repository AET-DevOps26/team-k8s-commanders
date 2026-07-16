{
  description = "dev shell";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      pkgs = import nixpkgs {
        system = "x86_64-linux";
        config.allowUnfreePredicate = pkg: builtins.elem (nixpkgs.lib.getName pkg) [ "terraform" ];
      };
    in {
      devShells.x86_64-linux.default = pkgs.mkShell {
        packages = with pkgs; [
          docker

          # Java / Spring Boot services (services/springboot, auth-service, notes-service, ...)
          jdk25
          maven

          # web-client (React + Vite + TypeScript)
          nodejs_24

          # ai-assistant (FastAPI)
          python314
          python314Packages.pip
          python314Packages.virtualenv
          ruff

          # Kubernetes deploy (Makefile / scripts/*.sh, helm/caredesk chart)
          kubectl
          kubernetes-helm
          kind

          # infra/terraform (azurerm provider)
          terraform

          # infra/ansible (VM provisioning)
          ansible

          git
        ];

        # api/scripts/gen-all.sh creates a venv at api/scripts/.venv and pip-installs
        # ruff into it, then puts that venv's bin/ first on PATH so
        # openapi-python-client can shell out to it. pip's ruff wheel ships a
        # generic-glibc binary that can't run on NixOS (no /lib64/ld-linux-*), so
        # pre-create that venv here and swap in the properly linked nix build —
        # this fixes it without touching the (checked-in, CI-shared) script.
        shellHook = ''
          repo_root="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
          gen_venv="$repo_root/api/scripts/.venv"
          if [ ! -d "$gen_venv" ]; then
            python3 -m venv "$gen_venv"
            "$gen_venv/bin/pip" install -q --disable-pip-version-check -r "$repo_root/api/scripts/gen-requirements.txt"
          fi
          ln -sf "${pkgs.ruff}/bin/ruff" "$gen_venv/bin/ruff"
        '';
      };
    };
}
