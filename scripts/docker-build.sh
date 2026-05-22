#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Building Docker images..."
docker compose build ingestion-service query-service

echo "Done."
echo "  enterprise-rag/ingestion-service:0.1.0"
echo "  enterprise-rag/query-service:0.1.0"
