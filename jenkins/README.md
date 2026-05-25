# Jenkins setup — Enterprise RAG

Two pipelines by **environment** (not per service). Each job builds **both** `ingestion-service` and `query-service`.

| Script | Job name (suggested) | Branch |
|--------|----------------------|--------|
| `jenkins/Jenkinsfile.dev` | `enterprise-rag-dev` | `develop` |
| `jenkins/Jenkinsfile.prod` | `enterprise-rag-prod` | `main` |

---

## Step 1 — Start Jenkins locally (Docker)

From repo root:

```bash
# Colima users: point at Colima's Docker socket
cp jenkins/.env.example jenkins/.env
# Edit jenkins/.env — uncomment:
#   DOCKER_SOCK=${HOME}/.colima/default/docker.sock

docker compose -f jenkins/docker-compose.yml up -d --build
```

Open **http://localhost:8080**

Initial admin password:

```bash
docker exec rag-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Complete the setup wizard (install suggested plugins is OK; our image already bundles core plugins).

---

## Step 2 — Global tool configuration

**Manage Jenkins → Tools**

| Tool | Name (must match) | Path / version |
|------|-------------------|----------------|
| JDK | `jdk-17` | Java 17 (bundled or `/opt/java/openjdk`) |

The Jenkins image is `lts-jdk17`; in the job you can use **JDK installer** or **System JDK** named exactly `jdk-17`.

---

## Step 3 — Credentials (minimum for first run)

**Manage Jenkins → Credentials → System → Global**

For a **first pipeline run without Sonar/deploy**, you can skip credentials and set job environment variables (Step 5).

When ready for full dev pipeline:

| ID | Type | Notes |
|----|------|--------|
| `docker-registry` | Username/password | Only if `DOCKER_REGISTRY` is set |
| `staging-ssh-key` | SSH private key | Required for EC2 Compose deploys |
| SonarQube server | — | Named **`SonarQube`** in Configure System (optional) |

**Kubernetes:** The dev Jenkinsfile runs `kubectl` on the agent. The local compose file mounts `~/.kube/config`. Ensure context works:

```bash
kubectl config use-context colima   # or your cluster
kubectl get nodes
```

---

## Step 4 — Create the dev Pipeline job

1. **New Item** → name: `enterprise-rag-dev` → **Pipeline** → OK  
2. **General** → optionally check **Discard old builds**  
3. **Pipeline** section:
   - **Definition:** Pipeline script from SCM  
   - **SCM:** Git  
   - **Repository URL:** your fork/clone URL (or `file:///workspace/enterprise-rag` is *not* supported for SCM — use a real Git remote or see “Local Git” below)  
   - **Branch:** `*/develop` or `*/main` (match your branch)  
   - **Script Path:** `jenkins/Jenkinsfile.dev`  
4. Save → **Build Now**

### Local Git remote (no GitHub yet)

```bash
cd /path/to/enterprise-rag
git init
git add .
git commit -m "initial"
# Use file:// URL in Jenkins only if Jenkins can read the path inside the container;
# easier: push to GitHub/GitLab and use HTTPS URL in the job.
```

---

## Step 5 — First build (recommended env vars)

**Job → Configure → Environment variables** (or **Pipeline** → environment in a wrapper — use **Build Environment** → **Inject environment variables**):

| Name | Value | Why |
|------|--------|-----|
| `ENABLE_SONAR` | `false` | No Sonar server yet |
| `ENABLE_DEPLOY` | `false` | Deploy optional for first run |
| `ENABLE_CHECKMARX` | `false` | Dev default |
| `DEPLOY_TARGET` | `ec2` | Preferred staging path |
| `DOCKER_REGISTRY` | `ghcr.io` | Registry host for pushed images |
| `IMAGE_NAMESPACE` | `codex-craftsman-labs` | Lowercase GHCR namespace |
| `STAGING_SSH_HOST` | `ec2-public-dns` | Target staging VM |
| `STAGING_SSH_USER` | `ubuntu` | Default Ubuntu EC2 user |
| `STAGING_PATH` | `/opt/enterprise-rag` | Remote compose directory |

Stages that will run:

1. Checkout  
2. Build & Test (`./gradlew clean build jacocoAggregatedReport`)  
3. Coverage gate  
4. Docker Build (both images)  
5. Skip Push / Deploy / Sonar  

When staging is ready:

```bash
DOCKER_REGISTRY=ghcr.io
IMAGE_NAMESPACE=codex-craftsman-labs
ENABLE_DEPLOY=true
```

The dev pipeline will then:

1. Push both service images to the configured registry
2. SSH to the EC2 host
3. Copy `docker-compose.yml` + `infra/`
4. Run `docker compose pull && docker compose up -d`

If you still want the old Kubernetes path:

```bash
DEPLOY_TARGET=k8s
ENABLE_DEPLOY=true
```

and ensure `kubectl apply -k k8s/overlays/dev` works from the host.

---

## Step 6 — Prod job (later)

Duplicate the dev job or create `enterprise-rag-prod`:

- **Script Path:** `jenkins/Jenkinsfile.prod`  
- **Branch:** `main`  
- Stricter: Sonar quality gate, optional Checkmarx, **manual approval** before prod deploy  

---

## Pipeline stages (reference)

```
Checkout
  → Build & Test (whole monorepo)
  → Coverage Gate
  → SonarQube (optional)
  → Docker Build  ← ingestion-service + query-service images
  → Push Images (if DOCKER_REGISTRY set)
  → Deploy to EC2 Compose (preferred) or Kubernetes
```

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `docker: not found` / permission denied | Use `jenkins/.env` `DOCKER_SOCK` for Colima; container runs as `root` in local compose |
| `kubectl: connection refused` | Start cluster: `colima start --kubernetes`; `kubectl config use-context colima` |
| `jdk-17` not found | Add JDK 17 tool named exactly `jdk-17` in Global Tools |
| Sonar fails | Set `ENABLE_SONAR=false` until SonarQube server is configured |
| EC2 deploy fails over SSH | Verify `staging-ssh-key`, `STAGING_SSH_HOST`, and that Docker is installed on the VM |
| EC2 deploy fails pulling images | Verify `docker-registry` credentials can pull from `DOCKER_REGISTRY` |
| Deploy fails (CrashLoop) | K8s overlay has no Postgres/Kafka/Ollama — use Docker Compose for full stack or add infra manifests |
| Gradle OOM on agent | Job env: `GRADLE_OPTS=-Dorg.gradle.daemon=false -Xmx1024m` |

---

## Stop Jenkins

```bash
docker compose -f jenkins/docker-compose.yml down
# remove data: docker volume rm jenkins_jenkins_home
```

---

## Related

- GitHub Actions (no Jenkins required): `.github/workflows/ci.yml`  
- K8s deploy: `kubectl apply -k k8s/overlays/dev`  
- Full runbook (local): `docs/getting-started/run-playbook.md`
