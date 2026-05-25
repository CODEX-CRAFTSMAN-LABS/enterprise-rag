#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

: "${STAGING_SSH_HOST:?Set STAGING_SSH_HOST to the EC2 public host or IP.}"
: "${INGESTION_IMAGE:?Set INGESTION_IMAGE to the published ingestion image tag.}"
: "${QUERY_IMAGE:?Set QUERY_IMAGE to the published query image tag.}"
: "${REGISTRY_USERNAME:?Set REGISTRY_USERNAME to the container registry username.}"
: "${REGISTRY_PASSWORD:?Set REGISTRY_PASSWORD to the container registry password or token.}"

STAGING_SSH_USER="${STAGING_SSH_USER:-ubuntu}"
STAGING_PATH="${STAGING_PATH:-/opt/enterprise-rag}"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-ghcr.io}"
COMPOSE_PROFILES="${COMPOSE_PROFILES:-}"

REMOTE="${STAGING_SSH_USER}@${STAGING_SSH_HOST}"
SSH_ARGS=(-o StrictHostKeyChecking=no)
if [[ -n "${SSH_KEY_PATH:-}" ]]; then
  SSH_ARGS=(-i "${SSH_KEY_PATH}" "${SSH_ARGS[@]}")
fi

tmp_env="$(mktemp)"
cleanup() {
  rm -f "${tmp_env}"
}
trap cleanup EXIT

cat >"${tmp_env}" <<EOF
INGESTION_IMAGE=${INGESTION_IMAGE}
QUERY_IMAGE=${QUERY_IMAGE}
COMPOSE_PROFILES=${COMPOSE_PROFILES}
EOF

ssh "${SSH_ARGS[@]}" "${REMOTE}" "sudo mkdir -p '${STAGING_PATH}' && sudo chown '${STAGING_SSH_USER}':'${STAGING_SSH_USER}' '${STAGING_PATH}'"
tar -C "${ROOT_DIR}" -cf - docker-compose.yml infra \
  | ssh "${SSH_ARGS[@]}" "${REMOTE}" "tar -xf - -C '${STAGING_PATH}'"
scp "${SSH_ARGS[@]}" "${tmp_env}" "${REMOTE}:${STAGING_PATH}/staging.env"

ssh "${SSH_ARGS[@]}" "${REMOTE}" "
  echo '${REGISTRY_PASSWORD}' | docker login '${DOCKER_REGISTRY}' -u '${REGISTRY_USERNAME}' --password-stdin &&
  cd '${STAGING_PATH}' &&
  docker compose --env-file staging.env pull ingestion-service query-service &&
  docker compose --env-file staging.env up -d postgres redis kafka ollama ingestion-service query-service &&
  docker compose --env-file staging.env ps
"
