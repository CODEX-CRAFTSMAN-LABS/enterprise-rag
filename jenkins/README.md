# Jenkins Setup - Enterprise RAG

The recommended deployment path is now:

1. Jenkins runs on its own EC2 instance
2. Jenkins builds both services
3. Jenkins pushes immutable SHA-tagged images to ECR
4. Jenkins deploys those exact images to the app EC2 host with Docker Compose

Pipelines by environment:

| Script | Job (seed path) | Purpose |
|--------|-----------------|---------|
| `jenkins/Jenkinsfile.dev` | `deploy/dev/enterprise-rag` | Full build, test, push ECR, deploy |
| `jenkins/Jenkinsfile.deploy-ecr` | `deploy/dev/enterprise-rag-from-ecr` | Deploy only — images already in ECR (e.g. from GitHub Actions) |
| `jenkins/Jenkinsfile.prod` | `deploy/prod/enterprise-rag` | Production build + K8s deploy |

**Shared ECR registry** (same tags for GitHub Actions and Jenkins):

```text
440977419877.dkr.ecr.ap-south-1.amazonaws.com/enterprise-rag-ingestion-service:<full-git-commit-sha>
440977419877.dkr.ecr.ap-south-1.amazonaws.com/enterprise-rag-query-service:<full-git-commit-sha>
```

```text
GitHub Actions (push) ──► ECR ◄── Jenkins (build+push OR deploy-only)
                              │
                              ▼
                         App EC2 (docker compose pull)
```

If you want a Jenkins UI layout similar to your screenshots, use the included seed-job flow. It generates:

```text
deploy/
  dev/
    enterprise-rag
  prod/
    enterprise-rag
```

## Recommended Topology

```text
GitHub -> Jenkins EC2 -> Amazon ECR -> App EC2 -> Docker Compose
```

Use a separate Jenkins EC2 instance so CI work does not compete with Postgres, Kafka, Redis, Ollama, and the Java services running on the app host.

## Step 1 - Bootstrap the Jenkins EC2 host

If you want to launch the Jenkins EC2 instance from AWS CLI first:

```bash
export AWS_REGION=<your-region>
export AMI_ID=<ubuntu-ami-id>
export SUBNET_ID=<subnet-id>
export SECURITY_GROUP_ID=<sg-id>
export KEY_NAME=<ec2-key-name>
export IAM_INSTANCE_PROFILE=<jenkins-instance-profile-name>
./scripts/provision-jenkins-ec2.sh
```

Then SSH into the created host and continue with the bootstrap below.

Launch a dedicated Ubuntu EC2 instance for Jenkins, SSH into it as `ubuntu`, and run:

```bash
chmod +x scripts/bootstrap-jenkins-ubuntu.sh
./scripts/bootstrap-jenkins-ubuntu.sh
```

That script installs:

- Jenkins
- Docker / Docker Compose
- Java 17
- AWS CLI
- Git / SSH client / jq / unzip

It also adds both your login user and the `jenkins` user to the `docker` group.

After the script finishes:

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

Open `http://<jenkins-ec2-public-ip>:8080`.

After first login, install these plugins from **Manage Jenkins -> Plugins**:

- `Job DSL`
- `Folders`
- `Pipeline: Stage View`
- `Git`
- `Docker Pipeline`
- `Credentials Binding`
- `Timestamper`
- `Workspace Cleanup`
- `JUnit`
- `SonarQube Scanner`

## Step 2 - Attach AWS IAM roles

### Jenkins EC2 IAM role

Attach an IAM role that can:

- push to ECR
- read ECR repo metadata

Minimum capabilities:

- `ecr:GetAuthorizationToken`
- `ecr:BatchCheckLayerAvailability`
- `ecr:CompleteLayerUpload`
- `ecr:DescribeRepositories`
- `ecr:CreateRepository`
- `ecr:InitiateLayerUpload`
- `ecr:PutImage`
- `ecr:UploadLayerPart`
- `ecr:PutLifecyclePolicy`

### App EC2 IAM role

Attach an IAM role that can pull from ECR:

- `ecr:GetAuthorizationToken`
- `ecr:BatchGetImage`
- `ecr:GetDownloadUrlForLayer`
- `ecr:BatchCheckLayerAvailability`

The app EC2 bootstrap script now installs AWS CLI so the deploy script can log in to ECR on-host.

## Step 3 - Create the ECR repositories

From Jenkins EC2, your laptop, or any machine with AWS credentials:

```bash
export AWS_REGION=<your-region>
./scripts/create-ecr-repos.sh
```

Default repositories:

- `enterprise-rag-ingestion-service`
- `enterprise-rag-query-service`

The script creates missing repositories as immutable and applies a basic lifecycle policy.

## Step 4 - Jenkins global tools

In **Manage Jenkins -> Tools**:

| Tool | Name (must match) | Value |
|------|-------------------|-------|
| JDK | `jdk-17` | Java 17 |

The pipeline expects the JDK tool name to be exactly `jdk-17`.

## Step 5 - Jenkins credentials

In **Manage Jenkins -> Credentials -> System -> Global** add:

| ID | Type | Required | Notes |
|----|------|----------|-------|
| `staging-ssh-key` | SSH private key | Yes | Private key that Jenkins uses to SSH into the app EC2 host |
| SonarQube server | Jenkins system config | Optional | Named `SonarQube` if you want the Jenkins Sonar stage enabled |

You no longer need the old `docker-registry` username/password credential for the ECR path.

## Step 6 - Create the Jenkins job

1. **New Item** -> `enterprise-rag-dev` -> **Pipeline**
2. In **Pipeline**:
   - **Definition:** Pipeline script from SCM
   - **SCM:** Git
   - **Repository URL:** your GitHub repository URL
   - **Branch:** `*/develop` or the branch you want Jenkins to track
   - **Script Path:** `jenkins/Jenkinsfile.dev`
3. Save

## Optional - Create the exact foldered Jenkins setup

To get a structure like the screenshots you shared, create one seed job and let it generate the real jobs.

### Create the seed job

1. **New Item** -> `seed-job` -> **Pipeline**
2. In **Pipeline**:
   - **Definition:** Pipeline script from SCM
   - **SCM:** Git
   - **Repository URL:** `https://github.com/CODEX-CRAFTSMAN-LABS/enterprise-rag.git`
   - **Branch:** `*/main`
   - **Script Path:** `jenkins/Jenkinsfile.seed`
3. Save
4. Open `seed-job` -> **Configure** -> add these environment variables if needed:

| Name | Default |
|------|---------|
| `GIT_REPO_URL` | `https://github.com/CODEX-CRAFTSMAN-LABS/enterprise-rag.git` |
| `DEV_BRANCH` | `develop` |
| `PROD_BRANCH` | `main` |

5. Click **Build Now**

After the seed job runs, Jenkins will create:

- `deploy/dev/enterprise-rag` — full build + ECR push + deploy
- `deploy/dev/enterprise-rag-from-ecr` — deploy existing ECR tag (from GitHub Actions)
- `deploy/prod/enterprise-rag`

This is the easiest way to mirror the same style of Jenkins layout shown in your photos.

## GitHub Actions + Jenkins (same ECR)

1. Configure GitHub repo secrets/variables (see [.github/README.md](../.github/README.md)).
2. Push to `main` or `develop` — Actions pushes the **full Git commit SHA** as the ECR tag (plus `sha-<12>` and branch aliases on the same digest).
3. In Jenkins, run **`deploy/dev/enterprise-rag-from-ecr`** with **`IMAGE_TAG`** = full commit SHA from the Actions log.
4. Or run **`deploy/dev/enterprise-rag`** to build on Jenkins, push to the same ECR repos, and deploy.

Both paths use `scripts/deploy-ec2-compose.sh`, which pulls from ECR on the app host.

## Step 7 - Required job environment variables

Set these in the job configuration or the Jenkins folder/job environment:

| Name | Example | Why |
|------|---------|-----|
| `AWS_REGION` | `eu-north-1` | Region for ECR auth and repo creation |
| `AWS_ACCOUNT_ID` | `123456789012` | Used to derive the ECR registry if `ECR_REGISTRY` is not set |
| `ECR_REGISTRY` | `123456789012.dkr.ecr.eu-north-1.amazonaws.com` | Optional explicit registry override |
| `ENABLE_DEPLOY` | `true` | Enables the EC2 deployment stage |
| `ENABLE_SONAR` | `true` or `false` | Enables the Sonar stage in Jenkins |
| `DEPLOY_TARGET` | `ec2` | Keeps the dev job on the EC2 Compose path |
| `STAGING_SSH_HOST` | `ec2-public-dns-or-ip` | App EC2 host Jenkins deploys to |
| `STAGING_SSH_USER` | `ubuntu` | Default Ubuntu EC2 SSH user |
| `STAGING_PATH` | `/opt/enterprise-rag` | Remote deployment directory |
| `OLLAMA_IMAGE` | `alpine/ollama:0.23.2` | Smaller CPU-only image for small EC2 hosts |

## Step 8 - Bootstrap the app EC2 host

On the app EC2 instance:

```bash
chmod +x scripts/bootstrap-ec2-ubuntu.sh
./scripts/bootstrap-ec2-ubuntu.sh
```

That script now installs Docker, Docker Compose, and AWS CLI. Attach the ECR pull IAM role before running Jenkins deployments.

## What the dev pipeline now does

`jenkins/Jenkinsfile.dev` now follows this flow:

1. Checkout
2. Build and test
3. Coverage gate
4. Optional Sonar
5. Tag images with full Git commit SHA (same tag GitHub Actions uses)
6. Ensure ECR repositories exist
7. Build both service images
8. Push SHA-tagged images to ECR
9. Push branch tag for convenience
10. Deploy the exact SHA-tagged images to the app EC2 host
11. Wait for `ingestion-service` and `query-service` health checks to pass

## Local Jenkins (optional)

For local Jenkins container testing:

```bash
cp jenkins/.env.example jenkins/.env
docker compose -f jenkins/docker-compose.yml up -d --build
```

The local Jenkins container now includes:

- AWS CLI
- Docker
- kubectl
- Git / SSH client

If you want local ECR access, mount your local AWS config by setting:

```bash
AWS_CONFIG=${HOME}/.aws
```

in `jenkins/.env`.

## Deployment Files Used

Important files in the ECR -> EC2 flow:

- `jenkins/Jenkinsfile.dev`
- `jenkins/Jenkinsfile.seed`
- `jenkins/jobs/seed.groovy`
- `scripts/provision-jenkins-ec2.sh`
- `scripts/create-ecr-repos.sh`
- `scripts/deploy-ec2-compose.sh`
- `scripts/verify-compose-health.sh`
- `scripts/bootstrap-jenkins-ubuntu.sh`
- `scripts/bootstrap-ec2-ubuntu.sh`
- `docker-compose.yml`
- `docker-compose.ec2.override.yml`

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `Unable to locate credentials` in Jenkins | Verify the Jenkins EC2 instance role or AWS credentials on the host |
| `docker: permission denied` on Jenkins EC2 | Re-login after group changes or run `newgrp docker` |
| `docker login` to ECR fails on app EC2 | Verify AWS CLI is installed and the app EC2 IAM role has ECR pull permissions |
| Deploy succeeds but services stay unhealthy | Run `docker compose ps` and `docker logs` on the app EC2 host; the deploy script now prints service logs on health-check failure |
| `jdk-17` not found | Configure Jenkins global tools with the exact name `jdk-17` |
| Sonar fails in Jenkins | Set `ENABLE_SONAR=false` until the Jenkins Sonar server configuration exists |
| EC2 host is too small for default Ollama image | Keep `OLLAMA_IMAGE=alpine/ollama:0.23.2` and use `docker-compose.ec2.override.yml` |

## Deferred Domain / HTTPS Phase

Domain mapping was intentionally deferred so deployment can stabilize first. The follow-up runbook is in `jenkins/DOMAIN-HTTPS.md`.

## Stop Local Jenkins

```bash
docker compose -f jenkins/docker-compose.yml down
```

## Related

- GitHub Actions CI: `.github/workflows/ci.yml`
- EC2 deploy script: `scripts/deploy-ec2-compose.sh`
- App host bootstrap: `scripts/bootstrap-ec2-ubuntu.sh`
