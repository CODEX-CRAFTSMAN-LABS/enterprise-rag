# Postman — Enterprise RAG

## Import

1. Open **Postman** → **Import**
2. Select:
   - `Enterprise-RAG.postman_collection.json`
   - `Enterprise-RAG.postman_environment.json` (Docker Compose on localhost)
3. Choose environment **Enterprise RAG — Local (Docker Compose)** in the top-right dropdown.

## Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `tenantId` | `acme-corp` | Sent as `X-Tenant-Id` on all secured requests |
| `ingestionBaseUrl` | `http://localhost:8081` | Ingestion service |
| `queryBaseUrl` | `http://localhost:8082` | Query service |
| `documentId` | *(set by upload test script)* | Last uploaded document id |

Collection-level auth applies `X-Tenant-Id: {{tenantId}}` automatically. Health and actuator requests override auth to **No Auth**.

## E2E test flow

1. Start stack: `./scripts/docker-up.sh` or `docker compose up -d`
2. Run folder **E2E — Upload then Ask** in order:
   - **1. Upload document** — in Body → form-data → `file`, pick a `.md` / `.txt` / `.pdf` from disk
   - Wait for indexing: `docker compose logs -f ingestion-service` (until saga completes)
   - **2. Ask question**
3. If `citations` is empty, indexing is not finished yet — wait and retry Ask.

## Kubernetes (dev)

Port-forward services, then use the same environment URLs:

```bash
kubectl port-forward -n enterprise-rag-dev svc/ingestion-service 8081:8081
kubectl port-forward -n enterprise-rag-dev svc/query-service 8082:8082
```

Optional: import `Enterprise-RAG-K8s-Dev.postman_environment.json` (same localhost ports after forward).

## Swagger (alternative to Postman)

| Service | Swagger UI |
|---------|------------|
| Ingestion | http://localhost:8081/swagger-ui/index.html |
| Query | http://localhost:8082/swagger-ui/index.html |

Authorize with `X-Tenant-Id` = `acme-corp` in each UI.
