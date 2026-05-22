#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ "${CONFIRM_PROD:-}" != "yes" ]]; then
  echo "Set CONFIRM_PROD=yes to deploy to production."
  echo "  CONFIRM_PROD=yes ./scripts/k8s-deploy-prod.sh"
  exit 1
fi

echo "==> Building images (prod tags)..."
docker build -f ingestion-service/Dockerfile -t enterprise-rag/ingestion-service:prod .
docker build -f query-service/Dockerfile -t enterprise-rag/query-service:prod .

echo "==> Applying k8s/overlays/prod..."
kubectl apply -k k8s/overlays/prod

echo "==> Waiting for rollouts..."
kubectl rollout status deployment/ingestion-service -n enterprise-rag-prod --timeout=600s
kubectl rollout status deployment/query-service -n enterprise-rag-prod --timeout=600s

echo "Production deploy submitted. Verify Grafana secret was rotated from placeholder."
