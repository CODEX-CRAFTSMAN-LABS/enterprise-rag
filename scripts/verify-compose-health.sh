#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${1:-$(pwd)}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-60}"
SLEEP_SECONDS="${SLEEP_SECONDS:-10}"

compose_args=(-f "${PROJECT_DIR}/docker-compose.yml")
if [[ -f "${PROJECT_DIR}/docker-compose.ec2.override.yml" ]]; then
  compose_args+=(-f "${PROJECT_DIR}/docker-compose.ec2.override.yml")
fi
if [[ -f "${PROJECT_DIR}/staging.env" ]]; then
  compose_args+=(--env-file "${PROJECT_DIR}/staging.env")
fi

check_health() {
  local container_name="$1"
  docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${container_name}" 2>/dev/null || true
}

for ((attempt = 1; attempt <= MAX_ATTEMPTS; attempt++)); do
  ingestion_status="$(check_health rag-ingestion-service)"
  query_status="$(check_health rag-query-service)"

  echo "Attempt ${attempt}/${MAX_ATTEMPTS}: ingestion=${ingestion_status} query=${query_status}"

  if [[ "${ingestion_status}" == "healthy" && "${query_status}" == "healthy" ]]; then
    curl -fsS http://localhost:8081/actuator/health >/dev/null
    curl -fsS http://localhost:8082/actuator/health >/dev/null
    echo "Application health checks passed."
    exit 0
  fi

  sleep "${SLEEP_SECONDS}"
done

echo "Application health checks did not become healthy in time."
docker compose "${compose_args[@]}" ps || true
echo "--- rag-ingestion-service logs ---"
docker logs --tail 100 rag-ingestion-service || true
echo "--- rag-query-service logs ---"
docker logs --tail 100 rag-query-service || true
exit 1
