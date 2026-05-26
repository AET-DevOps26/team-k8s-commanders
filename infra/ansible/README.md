# Ansible — Docker Provisioning

Configures the CareDesk Azure VM (provisioned by Terraform) with Docker Engine and the Compose plugin.

## Prerequisites

- [Ansible](https://docs.ansible.com/ansible/latest/installation_guide/index.html) >= 2.14
- The SSH private key at `infra/terraform/caredesk_key` (generated during Terraform setup)
- The VM already running (see `infra/terraform/`)

## Files

| File | Purpose |
|---|---|
| `playbook.yml` | Installs Docker CE, Compose plugin, and adds the admin user to the `docker` group |
| `ansible.cfg` | Points to `inventory.ini` and the Terraform SSH key; accepts new host keys automatically (`StrictHostKeyChecking=accept-new`) |
| `inventory.ini` | `caredesk-dev` host entry — **not committed** (gitignored); create from the Terraform output |

## Setup

**1. Create the inventory file**

After `terraform apply`, grab the public IP from the output and create `inventory.ini`:

```ini
[vm]
caredesk-dev ansible_host=<public-ip>
```

**2. Run the playbook**

```bash
cd infra/ansible
ansible-playbook playbook.yml
```

Ansible connects as `azureuser` using `infra/terraform/caredesk_key` and installs:
- `docker-ce`, `docker-ce-cli`, `containerd.io`
- `docker-buildx-plugin`, `docker-compose-plugin`

The admin user is added to the `docker` group so Docker commands work without `sudo`.

## Verify

```bash
ssh -i ../terraform/caredesk_key azureuser@<public-ip> docker version
```
