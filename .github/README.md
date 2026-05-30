# GitHub Actions

## Workflow: `ci.yml`

| Trigger | Branches |
|---------|----------|
| **push** | All branches |
| **pull_request** | All base branches |
| **workflow_dispatch** | Manual run from Actions tab |

## Jobs

1. **Build & Test** — Spotless, `./gradlew build`, JaCoCo, coverage gate (25%), test report, PR coverage comment  
2. **SonarQube** — Only if repo variable `SONAR_ENABLED=true` and secret `SONAR_TOKEN`  
3. **Docker Build** — Builds both service images; pushes to **Amazon ECR** on push (not on PRs) when AWS secrets are configured

## First-time setup

1. Create a GitHub repo and push:

```bash
git remote add origin https://github.com/YOUR_USER/enterprise-rag.git
git push -u origin main
```

2. Open **Actions** tab — workflow runs on every push to any branch.

3. Before push, format locally:

```bash
./gradlew spotlessApply
```

## Amazon ECR push (GitHub Actions → same registry as Jenkins)

Repository **Settings → Secrets and variables → Actions**:

**Secrets**

| Name | Value |
|------|--------|
| `AWS_ACCESS_KEY_ID` | IAM user access key (e.g. `codexcraftsman`) |
| `AWS_SECRET_ACCESS_KEY` | IAM user secret key |

**Variables**

| Name | Example |
|------|---------|
| `AWS_REGION` | `ap-south-1` |
| `AWS_ACCOUNT_ID` | `440977419877` |
| `ECR_PUSH_ENABLED` | `true` (set `false` to skip ECR push) |

After a successful push to `main` or `develop`, the workflow log shows:

```text
Use IMAGE_TAG=<full-40-char-git-commit-sha> in Jenkins deploy job (copy from Actions log).
```

Use that tag in Jenkins job **`deploy/dev/enterprise-rag-from-ecr`** (see [jenkins/README.md](../jenkins/README.md)).

IAM user needs `AmazonEC2ContainerRegistryPowerUser` (or equivalent ECR push permissions).

## Optional: SonarCloud

Repository **Settings → Secrets and variables → Actions**:

- Secret: `SONAR_TOKEN`
- Variable: `SONAR_ENABLED` = `true`
