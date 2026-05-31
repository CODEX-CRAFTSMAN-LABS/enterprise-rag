#!/usr/bin/env bash
# End-to-end Jenkins setup for enterprise-rag (ap-south-1 / per-service deploy).
#
# Prerequisites:
#   - AWS CLI configured (account 440977419877 or your own)
#   - jenkins/setup.env filled in (copy from jenkins/setup.env.example)
#   - IAM instance profiles created (see jenkins/iam/*.json)
#
# Usage:
#   cp jenkins/setup.env.example jenkins/setup.env
#   $EDITOR jenkins/setup.env
#   ./scripts/setup-jenkins-full.sh
set -euo pipefail

export AWS_PAGER=""

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SETUP_ENV="${ROOT_DIR}/jenkins/setup.env"

if [[ ! -f "${SETUP_ENV}" ]]; then
  echo "Missing ${SETUP_ENV}"
  echo "Run: cp jenkins/setup.env.example jenkins/setup.env && edit it"
  exit 1
fi

# shellcheck disable=SC1090
source "${SETUP_ENV}"

: "${AWS_REGION:?Set AWS_REGION in jenkins/setup.env}"
: "${AWS_ACCOUNT_ID:?Set AWS_ACCOUNT_ID in jenkins/setup.env}"

preflight_provision_vars() {
  local missing=()
  local need_jenkins=false
  local need_app=false

  if [[ "${SKIP_PROVISION:-false}" == "true" ]]; then
    if [[ -z "${JENKINS_SSH_HOST:-}" ]]; then
      missing+=("JENKINS_SSH_HOST (required when SKIP_PROVISION=true)")
    fi
    if [[ -z "${STAGING_SSH_HOST:-}" ]]; then
      missing+=("STAGING_SSH_HOST (required when SKIP_PROVISION=true)")
    fi
  else
    if [[ "${PROVISION_JENKINS:-true}" == "true" && -z "${JENKINS_SSH_HOST:-}" ]]; then
      need_jenkins=true
    fi
    if [[ "${PROVISION_APP:-true}" == "true" && -z "${STAGING_SSH_HOST:-}" ]]; then
      need_app=true
    fi
    if [[ "${need_jenkins}" == "true" || "${need_app}" == "true" ]]; then
      [[ -z "${AMI_ID:-}" ]] && missing+=("AMI_ID")
      [[ -z "${SUBNET_ID:-}" ]] && missing+=("SUBNET_ID")
      [[ -z "${KEY_NAME:-}" ]] && missing+=("KEY_NAME")
    fi
    if [[ "${need_jenkins}" == "true" && -z "${JENKINS_SECURITY_GROUP_ID:-}" ]]; then
      missing+=("JENKINS_SECURITY_GROUP_ID")
    fi
    if [[ "${need_app}" == "true" && -z "${APP_SECURITY_GROUP_ID:-}" ]]; then
      missing+=("APP_SECURITY_GROUP_ID")
    fi
  fi

  if ((${#missing[@]} > 0)); then
    echo "Missing values in jenkins/setup.env:"
    printf '  - %s\n' "${missing[@]}"
    echo ""
    echo "Either fill EC2 provisioning fields (AMI_ID, SUBNET_ID, KEY_NAME, security groups)"
    echo "or set SKIP_PROVISION=true with existing JENKINS_SSH_HOST and STAGING_SSH_HOST."
    exit 1
  fi
}

preflight_provision_vars

expand_path() {
  local p="$1"
  p="${p/#\~/${HOME}}"
  printf '%s' "${p}"
}

ssh_cmd() {
  local host="$1"
  local user="$2"
  local key="$3"
  shift 3
  ssh -i "${key}" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=15 "${user}@${host}" "$@"
}

scp_to() {
  local host="$1"
  local user="$2"
  local key="$3"
  local src="$4"
  local dest="$5"
  scp -i "${key}" -o StrictHostKeyChecking=accept-new "${src}" "${user}@${host}:${dest}"
}

wait_for_ssh() {
  local host="$1"
  local user="$2"
  local key="$3"
  if [[ ! -f "${key}" ]]; then
    echo "SSH key not found: ${key}"
    echo "Set STAGING_SSH_KEY_PATH in jenkins/setup.env to your EC2 .pem file (KEY_NAME=${KEY_NAME:-?})."
    exit 1
  fi
  echo "Waiting for SSH on ${user}@${host} (key: ${key}) ..."
  for _ in $(seq 1 30); do
    if ssh_cmd "${host}" "${user}" "${key}" "echo ok" >/dev/null 2>&1; then
      echo "SSH ready: ${host}"
      return 0
    fi
    sleep 10
  done
  echo "SSH not ready on ${host}"
  echo "Check: security group allows port 22, KEY_NAME matches your .pem, path in STAGING_SSH_KEY_PATH"
  exit 1
}

echo "=== Step 1: Create ECR repositories ==="
export AWS_REGION
(cd "${ROOT_DIR}" && ./scripts/create-ecr-repos.sh)

JENKINS_HOST="${JENKINS_SSH_HOST:-}"
APP_HOST="${STAGING_SSH_HOST:-}"
SSH_KEY="$(expand_path "${JENKINS_SSH_KEY_PATH:-${STAGING_SSH_KEY_PATH:-~/.ssh/id_rsa}}")"

if [[ "${SKIP_PROVISION:-false}" != "true" ]]; then
  if [[ "${PROVISION_JENKINS:-true}" == "true" && -z "${JENKINS_HOST}" ]]; then
    echo ""
    echo "=== Step 2: Provision Jenkins EC2 ==="
    : "${AMI_ID:?Set AMI_ID in setup.env}"
    : "${SUBNET_ID:?Set SUBNET_ID in setup.env}"
    : "${JENKINS_SECURITY_GROUP_ID:?Set JENKINS_SECURITY_GROUP_ID in setup.env}"
    : "${KEY_NAME:?Set KEY_NAME in setup.env}"

    export AMI_ID SUBNET_ID KEY_NAME
    export SECURITY_GROUP_ID="${JENKINS_SECURITY_GROUP_ID}"
    export IAM_INSTANCE_PROFILE="${JENKINS_IAM_INSTANCE_PROFILE:-}"
    export INSTANCE_TYPE="${JENKINS_INSTANCE_TYPE:-t3.small}"
    export INSTANCE_NAME="${JENKINS_INSTANCE_NAME:-enterprise-rag-jenkins}"

    provision_out="$(cd "${ROOT_DIR}" && ./scripts/provision-jenkins-ec2.sh)"
    echo "${provision_out}"
    JENKINS_HOST="$(echo "${provision_out}" | awk '/Public IP:/ {print $3}')"
    echo "JENKINS_SSH_HOST=${JENKINS_HOST}" >> "${SETUP_ENV}.generated"
  fi

  if [[ "${PROVISION_APP:-true}" == "true" && -z "${APP_HOST}" ]]; then
    echo ""
    echo "=== Step 3: Provision App EC2 ==="
    : "${AMI_ID:?Set AMI_ID in setup.env}"
    : "${SUBNET_ID:?Set SUBNET_ID in setup.env}"
    : "${APP_SECURITY_GROUP_ID:?Set APP_SECURITY_GROUP_ID in setup.env}"
    : "${KEY_NAME:?Set KEY_NAME in setup.env}"

    export AMI_ID SUBNET_ID KEY_NAME APP_SECURITY_GROUP_ID
    export APP_IAM_INSTANCE_PROFILE="${APP_IAM_INSTANCE_PROFILE:-}"
    export APP_INSTANCE_TYPE="${APP_INSTANCE_TYPE:-t3.xlarge}"
    export APP_INSTANCE_NAME="${APP_INSTANCE_NAME:-enterprise-rag-app}"

    app_out="$(cd "${ROOT_DIR}" && ./scripts/provision-app-ec2.sh)"
    echo "${app_out}"
    APP_HOST="$(echo "${app_out}" | awk '/Public IP:/ {print $3}')"
    echo "STAGING_SSH_HOST=${APP_HOST}" >> "${SETUP_ENV}.generated"
  fi
fi

if [[ -z "${JENKINS_HOST}" ]]; then
  JENKINS_HOST="${JENKINS_SSH_HOST:-}"
fi
if [[ -z "${APP_HOST}" ]]; then
  APP_HOST="${STAGING_SSH_HOST:-}"
fi

JENKINS_USER="${JENKINS_SSH_USER:-ubuntu}"
APP_USER="${STAGING_SSH_USER:-ubuntu}"

if [[ -n "${JENKINS_HOST}" ]]; then
  echo ""
  echo "=== Step 4: Bootstrap Jenkins EC2 (${JENKINS_HOST}) ==="
  wait_for_ssh "${JENKINS_HOST}" "${JENKINS_USER}" "${SSH_KEY}"

  ssh_cmd "${JENKINS_HOST}" "${JENKINS_USER}" "${SSH_KEY}" \
    "test -d enterprise-rag || git clone '${GIT_REPO_URL}' enterprise-rag"

  ssh_cmd "${JENKINS_HOST}" "${JENKINS_USER}" "${SSH_KEY}" \
    "cd enterprise-rag && chmod +x scripts/*.sh && ./scripts/bootstrap-jenkins-ubuntu.sh"

  echo ""
  echo "=== Step 5: Install Jenkins plugins ==="
  ssh_cmd "${JENKINS_HOST}" "${JENKINS_USER}" "${SSH_KEY}" \
    "cd enterprise-rag && ./scripts/install-jenkins-plugins.sh"

  echo ""
  echo "=== Step 6: Copy setup.env to Jenkins host ==="
  scp_to "${JENKINS_HOST}" "${JENKINS_USER}" "${SSH_KEY}" "${SETUP_ENV}" "enterprise-rag/jenkins/setup.env"
  if [[ -n "${APP_HOST}" ]]; then
    ssh_cmd "${JENKINS_HOST}" "${JENKINS_USER}" "${SSH_KEY}" \
      "grep -q '^STAGING_SSH_HOST=' enterprise-rag/jenkins/setup.env && sed -i 's/^STAGING_SSH_HOST=.*/STAGING_SSH_HOST=${APP_HOST}/' enterprise-rag/jenkins/setup.env || echo 'STAGING_SSH_HOST=${APP_HOST}' >> enterprise-rag/jenkins/setup.env"
  fi
fi

if [[ -n "${APP_HOST}" ]]; then
  echo ""
  echo "=== Step 7: Bootstrap App EC2 (${APP_HOST}) ==="
  wait_for_ssh "${APP_HOST}" "${APP_USER}" "${SSH_KEY}"

  ssh_cmd "${APP_HOST}" "${APP_USER}" "${SSH_KEY}" \
    "test -d enterprise-rag || git clone '${GIT_REPO_URL}' enterprise-rag"

  ssh_cmd "${APP_HOST}" "${APP_USER}" "${SSH_KEY}" \
    "cd enterprise-rag && chmod +x scripts/*.sh && ./scripts/bootstrap-ec2-ubuntu.sh"
fi

echo ""
echo "=== Step 8: Jenkins UI first login (one-time) ==="
if [[ -n "${JENKINS_HOST}" ]]; then
  echo "  1. Open http://${JENKINS_HOST}:8080"
  echo "  2. Initial password:"
  ssh_cmd "${JENKINS_HOST}" "${JENKINS_USER}" "${SSH_KEY}" \
    "sudo cat /var/lib/jenkins/secrets/initialAdminPassword" || true
  echo "  3. Install suggested plugins OR skip (our script already installed plugins)"
  echo "  4. Create admin user"
  echo "  5. Create API token: User -> Security -> API Token -> Add new token"
  echo "  6. Set JENKINS_TOKEN in jenkins/setup.env on Jenkins host, then run:"
  echo "       cd enterprise-rag && ./scripts/jenkins-configure.sh"
else
  echo "  Set JENKINS_SSH_HOST in setup.env or complete Jenkins UI setup locally."
fi

echo ""
echo "=== Step 9: After jenkins-configure.sh ==="
echo "  Run deploy/dev/ingestion-service (Build with Parameters -> branch develop)"
echo "  Run deploy/dev/query-service"
echo ""
echo "ECR registry: ${ECR_REGISTRY:-${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com}"
if [[ -n "${JENKINS_HOST}" ]]; then
  echo "Jenkins UI: http://${JENKINS_HOST}:8080"
fi
if [[ -n "${APP_HOST}" ]]; then
  echo "App host: ${APP_HOST}"
fi
echo ""
echo "Generated values (if any): ${SETUP_ENV}.generated"
echo "Full runbook: jenkins/GO-LIVE.md"
