# Terraform — Azure VM

Provisions a single Ubuntu 24.04 VM on Azure with a public IP, VNet, and NSG (SSH + HTTP/S).

## Prerequisites

- [Terraform](https://developer.hashicorp.com/terraform/install) >= 1.5
- [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)

## Setup

**1. Log in to Azure**

```bash
az login
az account set --subscription "<your-subscription-id>"
```

**2. Create your tfvars**

```bash
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars` — the values you must fill in:

| Variable | Description |
|---|---|
| `subscription_id` | Your Azure subscription ID (`az account show --query id`) |

Everything else has a working default.

**3. Place your SSH key**

Put your ed25519 public key at `infra/terraform/caredesk_key.pub` (already gitignored).
Generate one if needed:

```bash
ssh-keygen -t ed25519 -f caredesk_key
```

**4. Deploy**

```bash
terraform init
terraform apply
```

Confirm the plan, then Terraform prints the public IP and SSH command when done.

**5. Connect**

```bash
ssh -i caredesk_key azureuser@<public-ip>
```

## Tear down

```bash
terraform destroy
```
