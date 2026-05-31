#!/usr/bin/env bash
# Fix EC2 SSH access when the local .pem does not match the AWS key pair.
#
# Creates enterprise-rag-v2 key (if missing), terminates unreachable instances,
# and launches replacements with the new key.
set -euo pipefail

export AWS_PAGER=""

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SETUP_ENV="${ROOT_DIR}/jenkins/setup.env"
KEY_NAME="${KEY_NAME:-enterprise-rag-v2}"
KEY_PATH="${KEY_PATH:-${HOME}/.ssh/${KEY_NAME}.pem}"
AWS_REGION="${AWS_REGION:-ap-south-1}"

if [[ -f "${SETUP_ENV}" ]]; then
  # shellcheck disable=SC1090
  source "${SETUP_ENV}"
fi

AWS_REGION="${AWS_REGION:-ap-south-1}"

if [[ ! -f "${KEY_PATH}" ]]; then
  echo "Creating key pair ${KEY_NAME} -> ${KEY_PATH}"
  aws ec2 create-key-pair \
    --region "${AWS_REGION}" \
    --key-name "${KEY_NAME}" \
    --query 'KeyMaterial' \
    --output text > "${KEY_PATH}"
  chmod 400 "${KEY_PATH}"
fi

: "${AMI_ID:?Set AMI_ID in jenkins/setup.env}"
: "${SUBNET_ID:?Set SUBNET_ID in jenkins/setup.env}"
: "${JENKINS_SECURITY_GROUP_ID:?Set JENKINS_SECURITY_GROUP_ID}"
: "${APP_SECURITY_GROUP_ID:?Set APP_SECURITY_GROUP_ID}"

terminate_if_running() {
  local id="$1"
  local state
  state="$(aws ec2 describe-instances --region "${AWS_REGION}" --instance-ids "${id}" \
    --query 'Reservations[0].Instances[0].State.Name' --output text 2>/dev/null || echo none)"
  if [[ "${state}" == "running" || "${state}" == "stopped" || "${state}" == "pending" ]]; then
    echo "Terminating ${id} (state=${state}) ..."
    aws ec2 terminate-instances --region "${AWS_REGION}" --instance-ids "${id}" >/dev/null
    aws ec2 wait instance-terminated --region "${AWS_REGION}" --instance-ids "${id}"
  fi
}

# Old instances launched with mismatched enterprise-rag key
for old_id in i-005a960e37b6645d6 i-05262970e0494072e; do
  terminate_if_running "${old_id}" || true
done

echo ""
echo "=== Launch App EC2 ==="
export AWS_REGION AMI_ID SUBNET_ID KEY_NAME
export APP_SECURITY_GROUP_ID
export APP_IAM_INSTANCE_PROFILE="${APP_IAM_INSTANCE_PROFILE:-enterprise-rag-ec2-ecr-pull}"
export APP_INSTANCE_TYPE="${APP_INSTANCE_TYPE:-t3.micro}"
export APP_INSTANCE_NAME="${APP_INSTANCE_NAME:-enterprise-rag-app}"
app_out="$("${ROOT_DIR}/scripts/provision-app-ec2.sh")"
echo "${app_out}"
APP_IP="$(echo "${app_out}" | awk '/Public IP:/ {print $3}')"

echo ""
echo "=== Launch Jenkins EC2 ==="
export SECURITY_GROUP_ID="${JENKINS_SECURITY_GROUP_ID}"
export IAM_INSTANCE_PROFILE="${JENKINS_IAM_INSTANCE_PROFILE:-enterprise-rag-jenkins-ecr-push}"
export INSTANCE_TYPE="${JENKINS_INSTANCE_TYPE:-t3.micro}"
export INSTANCE_NAME="${JENKINS_INSTANCE_NAME:-enterprise-rag-jenkins}"
jenkins_out="$("${ROOT_DIR}/scripts/provision-jenkins-ec2.sh")"
echo "${jenkins_out}"
JENKINS_IP="$(echo "${jenkins_out}" | awk '/Public IP:/ {print $3}')"

echo ""
echo "Waiting 45s for cloud-init ..."
sleep 45

ssh_test() {
  local ip="$1"
  for _ in $(seq 1 12); do
    if ssh -i "${KEY_PATH}" -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new "ubuntu@${ip}" "echo ok" >/dev/null 2>&1; then
      echo "SSH OK: ubuntu@${ip}"
      return 0
    fi
    sleep 10
  done
  echo "SSH failed: ubuntu@${ip}"
  return 1
}

ssh_test "${APP_IP}"
ssh_test "${JENKINS_IP}"

cat > "${SETUP_ENV}.generated" <<EOF
# Written by fix-ec2-ssh-access.sh $(date -u +%Y-%m-%dT%H:%M:%SZ)
KEY_NAME=${KEY_NAME}
STAGING_SSH_KEY_PATH=${KEY_PATH}
JENKINS_SSH_KEY_PATH=${KEY_PATH}
STAGING_SSH_HOST=${APP_IP}
JENKINS_SSH_HOST=${JENKINS_IP}
SKIP_PROVISION=true
PROVISION_JENKINS=false
PROVISION_APP=false
EOF

echo ""
echo "Update jenkins/setup.env with:"
echo "  KEY_NAME=${KEY_NAME}"
echo "  STAGING_SSH_KEY_PATH=${KEY_PATH}"
echo "  JENKINS_SSH_KEY_PATH=${KEY_PATH}"
echo "  STAGING_SSH_HOST=${APP_IP}"
echo "  JENKINS_SSH_HOST=${JENKINS_IP}"
echo "  SKIP_PROVISION=true"
echo ""
echo "Test:"
echo "  ssh -i \"${KEY_PATH}\" ubuntu@${APP_IP}"
echo "  ssh -i \"${KEY_PATH}\" ubuntu@${JENKINS_IP}"
echo ""
echo "Then run: ./scripts/setup-jenkins-full.sh"
