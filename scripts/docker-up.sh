#!/usr/bin/env bash
# Staged startup avoids Docker Compose "concurrent map writes" crash on some versions.
set -euo pipefail
cd "$(dirname "$0")/.."

export COMPOSE_PARALLEL_LIMIT="${COMPOSE_PARALLEL_LIMIT:-1}"

wait_healthy() {
  local container="$1"
  local max_attempts="${2:-45}"
  echo "  Waiting for ${container} to be healthy..."
  for _ in $(seq 1 "${max_attempts}"); do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container}" 2>/dev/null || echo missing)"
    if [[ "${status}" == "healthy" ]]; then
      echo "  ${container}: ${status}"
      return 0
    fi
    sleep 2
  done
  echo "  TIMEOUT: ${container} last status=${status}"
  docker logs "${container}" --tail 40 2>&1 || true
  return 1
}

wait_http() {
  local url="$1"
  local max_attempts="${2:-40}"
  echo "  Waiting for ${url} ..."
  for _ in $(seq 1 "${max_attempts}"); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      echo "  OK: ${url}"
      return 0
    fi
    sleep 3
  done
  echo "  TIMEOUT: ${url} (check logs for 'Killed' = out of memory)"
  return 1
}

echo "==> Clean stop (ignore errors from partial runs)..."
docker compose down --remove-orphans 2>/dev/null || true

echo "==> Build app images (once; may take several minutes first time)..."
docker compose build ingestion-service query-service

echo "==> Start infrastructure (no Grafana/Prometheus by default — saves RAM)..."
docker compose up -d postgres redis kafka ollama
wait_healthy rag-postgres 30
wait_healthy rag-redis 20
wait_healthy rag-kafka 40
wait_healthy rag-ollama 30

echo ""
if docker exec rag-ollama ollama list 2>/dev/null | grep -q nomic-embed-text \
   && docker exec rag-ollama ollama list 2>/dev/null | grep -q llama3.2; then
  echo "Ollama models already present — skipping ollama-init."
else
  echo "==> Pull Ollama models (first run only)..."
  docker compose --profile init run --rm ollama-init
fi

echo "==> Start ingestion-service..."
docker compose up -d ingestion-service
wait_healthy rag-ingestion-service 90
wait_http "http://localhost:8081/actuator/health" 40

echo "==> Start query-service..."
docker compose up -d query-service
wait_healthy rag-query-service 90
wait_http "http://localhost:8082/actuator/health" 40

if [[ "${MONITORING:-}" == "1" ]]; then
  echo "==> Start monitoring (Jaeger, Prometheus, Grafana)..."
  docker compose --profile monitoring up -d jaeger prometheus grafana
fi

echo ""
echo "Stack is up. Swagger UI:"
echo "  Ingestion: http://localhost:8081/swagger-ui/index.html"
echo "  Query:     http://localhost:8082/swagger-ui/index.html"
echo "  (also try /swagger-ui.html — redirects to index)"
echo ""
docker compose ps
echo ""
echo "If browser shows ERR_EMPTY_RESPONSE:"
echo "  docker compose logs ingestion-service query-service | grep -i killed"
echo "  → increase Colima memory: colima stop && colima start --memory 8 --cpu 4"
echo ""
echo "Logs: docker compose logs -f ingestion-service query-service"
