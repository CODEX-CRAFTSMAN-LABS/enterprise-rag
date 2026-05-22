#!/usr/bin/env bash
# Minimal stack for Swagger/API on low-RAM machines (stops monitoring + recreates apps with memory caps).
set -euo pipefail
cd "$(dirname "$0")/.."

echo "==> Stopping heavy optional services..."
docker compose stop grafana prometheus jaeger 2>/dev/null || true

echo "==> Recreating app containers with 512MB heap caps..."
docker compose up -d --force-recreate ingestion-service query-service

echo ""
echo "Wait 60–90 seconds, then test:"
echo "  curl http://localhost:8081/actuator/health"
echo "  curl http://localhost:8082/actuator/health"
echo ""
echo "Swagger:"
echo "  http://localhost:8081/swagger-ui/index.html"
echo "  http://localhost:8082/swagger-ui/index.html"
echo ""
echo "If still failing, Colima needs more RAM:"
echo "  colima stop && colima start --memory 8 --cpu 4"
