#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "==> Building images (dev tags)..."
docker build -f ingestion-service/Dockerfile -t enterprise-rag/ingestion-service:dev .
docker build -f query-service/Dockerfile -t enterprise-rag/query-service:dev .

if command -v minikube >/dev/null 2>&1 && minikube status >/dev/null 2>&1; then
  echo "==> Loading images into minikube..."
  minikube image load enterprise-rag/ingestion-service:dev
  minikube image load enterprise-rag/query-service:dev
elif kubectl config current-context 2>/dev/null | grep -q colima; then
  echo "==> Colima/k3s: using local images (imagePullPolicy: IfNotPresent)."
  echo "    If pods stay ImagePullBackOff, run: docker images | grep enterprise-rag"
fi

echo "==> Applying k8s/overlays/dev..."
kubectl apply -k k8s/overlays/dev

echo ""
echo "Done. Namespace: enterprise-rag-dev"
echo "  kubectl get pods -n enterprise-rag-dev"
echo "  kubectl port-forward -n enterprise-rag-dev svc/grafana 3000:3000"
echo "  Grafana: http://localhost:3000 (see grafana-admin secret)"
