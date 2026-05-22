# Kubernetes manifests

Use **Kustomize overlays** for environment-specific deployments.

| Environment | Path | Namespace |
|-------------|------|-----------|
| **Dev** | `overlays/dev/` | `enterprise-rag-dev` |
| **Prod** | `overlays/prod/` | `enterprise-rag-prod` |
| **Base** | `base/` | (set by overlay) |

```bash
# Dev
kubectl apply -k overlays/dev
# or
../scripts/k8s-deploy-dev.sh

# Prod (manual guard)
CONFIRM_PROD=yes ../scripts/k8s-deploy-prod.sh
```

Legacy flat manifests (`namespace.yaml`, `ingestion-service.yaml` in this folder root) are **deprecated** — use overlays.

**Prometheus:** Dev/prod overlays ship a **standalone Prometheus** deployment with static scrape configs (no Prometheus Operator required).

**ServiceMonitor** (optional): only if you install [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator) CRDs:

```bash
kubectl apply -k k8s/components/prometheus-operator
```

**Docs:** [docs/operations/kubernetes.md](../docs/operations/kubernetes.md) | [docs/operations/dev-prod-stack.md](../docs/operations/dev-prod-stack.md)
