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
3. **Docker Build** — Validates `docker compose` and builds both service images (no registry push)

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

## Optional: SonarCloud

Repository **Settings → Secrets and variables → Actions**:

- Secret: `SONAR_TOKEN`
- Variable: `SONAR_ENABLED` = `true`
