#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

: "${STAGING_SSH_HOST:?Set STAGING_SSH_HOST to the EC2 public host or IP.}"
: "${INGESTION_IMAGE:?Set INGESTION_IMAGE (tag :<git-commit-sha> or digest @sha256:...).}"
: "${QUERY_IMAGE:?Set QUERY_IMAGE (tag :<git-commit-sha> or digest @sha256:...).}"

STAGING_SSH_USER="${STAGING_SSH_USER:-ubuntu}"
STAGING_PATH="${STAGING_PATH:-/opt/enterprise-rag}"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-}"
AWS_REGION="${AWS_REGION:-}"
COMPOSE_PROFILES="${COMPOSE_PROFILES:-}"
OLLAMA_IMAGE="${OLLAMA_IMAGE:-}"
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

if [[ -n "${OLLAMA_IMAGE}" ]]; then
  printf 'OLLAMA_IMAGE=%s\n' "${OLLAMA_IMAGE}" >>"${tmp_env}"
fi

copy_targets=(
  docker-compose.yml
  infra
  scripts/verify-compose-health.sh
)
if [[ -f "${ROOT_DIR}/docker-compose.ec2.override.yml" ]]; then
  copy_targets+=(docker-compose.ec2.override.yml)
fi

ssh "${SSH_ARGS[@]}" "${REMOTE}" "sudo mkdir -p '${STAGING_PATH}/scripts' && sudo chown -R '${STAGING_SSH_USER}':'${STAGING_SSH_USER}' '${STAGING_PATH}'"
tar -C "${ROOT_DIR}" -cf - "${copy_targets[@]}" \
  | ssh "${SSH_ARGS[@]}" "${REMOTE}" "tar -xf - -C '${STAGING_PATH}'"
scp "${SSH_ARGS[@]}" "${tmp_env}" "${REMOTE}:${STAGING_PATH}/staging.env"

registry_password_escaped="$(printf '%s' "${REGISTRY_PASSWORD:-}" | sed "s/'/'\"'\"'/g")"
registry_username_escaped="$(printf '%s' "${REGISTRY_USERNAME:-}" | sed "s/'/'\"'\"'/g")"

ssh "${SSH_ARGS[@]}" "${REMOTE}" \
  "STAGING_PATH='${STAGING_PATH}' DOCKER_REGISTRY='${DOCKER_REGISTRY}' AWS_REGION='${AWS_REGION}' REGISTRY_USERNAME='${registry_username_escaped}' REGISTRY_PASSWORD='${registry_password_escaped}' bash -s" <<'EOF'
set -euo pipefail

cd "${STAGING_PATH}"
chmod +x scripts/verify-compose-health.sh

compose_args=(-f docker-compose.yml)
if [[ -f docker-compose.ec2.override.yml ]]; then
  compose_args+=(-f docker-compose.ec2.override.yml)
fi
compose_args+=(--env-file staging.env)

if [[ -n "${DOCKER_REGISTRY}" ]]; then
  if [[ "${DOCKER_REGISTRY}" == *.amazonaws.com ]]; then
    : "${AWS_REGION:?Set AWS_REGION when using Amazon ECR.}"
    if ! command -v aws >/dev/null 2>&1; then
      echo "AWS CLI is required on the app EC2 host to log in to ECR."
      exit 1
    fi
    aws ecr get-login-password --region "${AWS_REGION}" \
      | docker login --username AWS --password-stdin "${DOCKER_REGISTRY}"
  elif [[ -n "${REGISTRY_USERNAME}" && -n "${REGISTRY_PASSWORD}" ]]; then
    echo "${REGISTRY_PASSWORD}" | docker login "${DOCKER_REGISTRY}" -u "${REGISTRY_USERNAME}" --password-stdin
  else
    echo "Set REGISTRY_USERNAME and REGISTRY_PASSWORD for non-ECR registries."
    exit 1
  fi
fi

docker compose "${compose_args[@]}" pull ingestion-service query-service
docker compose "${compose_args[@]}" up -d postgres redis kafka ollama ingestion-service query-service
docker compose "${compose_args[@]}" ps
./scripts/verify-compose-health.sh "${STAGING_PATH}"
EOF
