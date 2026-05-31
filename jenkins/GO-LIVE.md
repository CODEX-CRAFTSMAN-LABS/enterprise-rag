# Go live on Jenkins — enterprise-rag (ap-south-1)

One-command setup after editing config:

```bash
cp jenkins/setup.env.example jenkins/setup.env
# Edit jenkins/setup.env — set AMI_ID, SUBNET_ID, security groups, KEY_NAME
./scripts/setup-jenkins-full.sh
```

Account defaults: **440977419877** · Region: **ap-south-1** · [AWS Console](https://440977419877.signin.aws.amazon.com/console)

---

## Architecture

```text
GitHub repo (one)
    │
    ├── seed-job  →  creates build/ + deploy/ jobs
    │
    ├── build/ingestion-service   →  Gradle + Docker → push ECR
    ├── build/query-service       →  Gradle + Docker → push ECR
    ├── deploy/dev/ingestion-service   →  pull ECR → deploy ingestion (dev)
    ├── deploy/dev/query-service       →  pull ECR → deploy query (dev)
    └── deploy/prod/...                →  same for prod (with approval)

Jenkins EC2  ──push──►  ECR (ap-south-1)  ──pull──►  App EC2 (docker compose)
```

---

## Phase 0 — AWS prerequisites (do once in console)

### IAM roles

Create two EC2 instance profiles using the JSON policies in this repo:

| Role | Policy file | Purpose |
|------|-------------|---------|
| Jenkins EC2 | `jenkins/iam/jenkins-ecr-push-policy.json` | Push images to ECR |
| App EC2 | `jenkins/iam/app-ecr-pull-policy.json` | Pull images on deploy |

Set profile names in `jenkins/setup.env`:

- `JENKINS_IAM_INSTANCE_PROFILE`
- `APP_IAM_INSTANCE_PROFILE`

### Security groups

| Host | Inbound |
|------|---------|
| Jenkins | 8080 from your IP, 22 from your IP |
| App | 22 from Jenkins SG (or Jenkins IP), app ports as needed |

### EC2 key pair

Use the same `KEY_NAME` for both instances. The private key path goes in `STAGING_SSH_KEY_PATH`.

### Ubuntu AMI

In **ap-south-1**, pick the latest Ubuntu 22.04 LTS AMI and set `AMI_ID` in `setup.env`.

---

## Phase 1 — Automated setup (from your laptop)

```bash
cp jenkins/setup.env.example jenkins/setup.env
```

Fill in at minimum:

```bash
AWS_REGION=ap-south-1
AWS_ACCOUNT_ID=440977419877
AMI_ID=ami-xxxxxxxx
SUBNET_ID=subnet-xxxxxxxx
JENKINS_SECURITY_GROUP_ID=sg-xxxxxxxx
APP_SECURITY_GROUP_ID=sg-xxxxxxxx
KEY_NAME=your-key
JENKINS_IAM_INSTANCE_PROFILE=enterprise-rag-jenkins-profile
APP_IAM_INSTANCE_PROFILE=enterprise-rag-app-profile
STAGING_SSH_KEY_PATH=~/.ssh/your-key.pem
```

Run:

```bash
chmod +x scripts/*.sh
./scripts/setup-jenkins-full.sh
```

This script:

1. Creates ECR repos (`enterprise-rag-ingestion-service`, `enterprise-rag-query-service`)
2. Provisions Jenkins EC2 + App EC2 (unless `SKIP_PROVISION=true`)
3. Bootstraps both hosts (Docker, Jenkins, AWS CLI)
4. Installs Jenkins plugins from `jenkins/plugins.txt`
5. Prints Jenkins initial admin password and next steps

---

## Phase 2 — Jenkins UI (one-time, ~5 min)

1. Open `http://<jenkins-ip>:8080`
2. Enter initial admin password (printed by setup script)
3. Skip plugin install if setup script already ran
4. Create admin user
5. **Security → API Token → Generate** → copy token

On Jenkins EC2:

```bash
cd enterprise-rag
nano jenkins/setup.env   # set JENKINS_TOKEN=<your-token>
./scripts/jenkins-configure.sh
```

This configures:

- JDK tool `jdk-17`
- Credential `staging-ssh-key`
- `seed-job` with your AWS/deploy settings
- Runs `seed-job` (creates all deploy jobs)

---

## Phase 3 — First deploy

In Jenkins UI:

1. **seed-job** should be green (creates jobs under `build/` and `deploy/dev/`)
2. Run **`build/ingestion-service`** with `GIT_COMMIT_SHA` (e.g. `git rev-parse HEAD`)
3. Run **`deploy/dev/ingestion-service`** with the **same** `GIT_COMMIT_SHA`
4. Repeat for **`build/query-service`** then **`deploy/dev/query-service`**

Build jobs: Gradle test + Docker push to ECR.

Deploy jobs: verify image in ECR, SSH to app EC2, restart **only that service**.

Verify on app host:

```bash
ssh ubuntu@<app-ip>
cd /opt/enterprise-rag
docker compose ps
```

---

## If hosts already exist

Set in `jenkins/setup.env`:

```bash
SKIP_PROVISION=true
JENKINS_SSH_HOST=<jenkins-ip>
STAGING_SSH_HOST=<app-ip>
```

Then run `./scripts/setup-jenkins-full.sh` (skips EC2 create, still bootstraps if needed).

---

## Scripts reference

| Script | Where to run | Purpose |
|--------|--------------|---------|
| `setup-jenkins-full.sh` | Laptop | Full orchestration |
| `provision-jenkins-ec2.sh` | Laptop | Jenkins EC2 only |
| `provision-app-ec2.sh` | Laptop | App EC2 only |
| `create-ecr-repos.sh` | Anywhere with AWS CLI | ECR repos |
| `bootstrap-jenkins-ubuntu.sh` | Jenkins EC2 | Install Jenkins + Docker |
| `bootstrap-ec2-ubuntu.sh` | App EC2 | Install Docker + AWS CLI |
| `install-jenkins-plugins.sh` | Jenkins EC2 | Plugins from plugins.txt |
| `jenkins-configure.sh` | Jenkins EC2 | JDK, SSH key, seed job |
| `approve-jenkins-seed-script.sh` | Laptop | Approve pending Job DSL script before seed-job |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Unable to locate credentials` | Attach Jenkins IAM role with ECR push |
| `docker: permission denied` | On Jenkins EC2: `sudo usermod -aG docker jenkins && sudo systemctl restart jenkins` |
| Deploy fails SSH | Check `staging-ssh-key` credential and app SG allows port 22 from Jenkins |
| `jdk-17` not found | Re-run `./scripts/jenkins-configure.sh` |
| Sonar fails | Keep `ENABLE_SONAR=false` in setup.env |
| Ollama OOM on small EC2 | `OLLAMA_IMAGE=alpine/ollama:0.23.2` (default in setup.env) |
| seed-job: `script not yet approved for use` | Run `./scripts/approve-jenkins-seed-script.sh` or **Manage Jenkins → In-process Script Approval → Approve**, then re-run seed-job |
| seed-job **UNSTABLE** (`envinject` needs to be installed) | Jobs are still created. Install plugin: `envinject` in `jenkins/plugins.txt`, run `install-jenkins-plugins.sh` on Jenkins EC2, re-run seed-job for green **SUCCESS** |

---

## Re-run seed after changing deploy target

Update `STAGING_SSH_HOST` in `jenkins/setup.env`, then:

```bash
./scripts/jenkins-configure.sh   # updates seed-job parameters
# Build seed-job again in Jenkins UI
```

All jobs under `deploy/dev/` and `deploy/prod/` will be regenerated with new env vars.
