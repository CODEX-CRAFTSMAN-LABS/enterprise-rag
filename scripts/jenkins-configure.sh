#!/usr/bin/env bash
# Configure Jenkins after bootstrap: JDK tool, SSH credential, seed job, optional seed run.
# Run ON the Jenkins EC2 host (or locally against JENKINS_URL).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SETUP_ENV="${ROOT_DIR}/jenkins/setup.env"

if [[ -f "${SETUP_ENV}" ]]; then
  # shellcheck disable=SC1090
  source "${SETUP_ENV}"
fi

JENKINS_URL="${JENKINS_URL:-http://127.0.0.1:8080}"
JENKINS_USER="${JENKINS_USER:-admin}"
JENKINS_TOKEN="${JENKINS_TOKEN:-}"
JENKINS_PASSWORD="${JENKINS_PASSWORD:-}"
RUN_SEED_JOB="${RUN_SEED_JOB:-true}"

STAGING_SSH_KEY_PATH="${STAGING_SSH_KEY_PATH:-${HOME}/.ssh/id_rsa}"
STAGING_SSH_KEY_PATH="${STAGING_SSH_KEY_PATH/#\~/${HOME}}"

auth_args=()
if [[ -n "${JENKINS_TOKEN}" ]]; then
  auth_args=(-u "${JENKINS_USER}:${JENKINS_TOKEN}")
elif [[ -n "${JENKINS_PASSWORD}" ]]; then
  auth_args=(-u "${JENKINS_USER}:${JENKINS_PASSWORD}")
else
  echo "Set JENKINS_TOKEN (preferred) or JENKINS_PASSWORD in jenkins/setup.env"
  echo "Create a token: Jenkins UI -> User -> Security -> API Token"
  exit 1
fi

wait_for_jenkins() {
  local attempts=60
  echo "Waiting for Jenkins at ${JENKINS_URL} ..."
  for ((i = 1; i <= attempts; i++)); do
    if curl -sf "${auth_args[@]}" "${JENKINS_URL}/api/json" >/dev/null 2>&1; then
      echo "Jenkins is up."
      return 0
    fi
    sleep 5
  done
  echo "Jenkins did not become ready in time."
  exit 1
}

get_crumb() {
  curl -sf "${auth_args[@]}" "${JENKINS_URL}/crumbIssuer/api/json" \
    | jq -r '.crumb // empty'
}

get_crumb_header() {
  curl -sf "${auth_args[@]}" "${JENKINS_URL}/crumbIssuer/api/json" \
    | jq -r '.crumbRequestField // empty'
}

run_script_console() {
  local script="$1"
  local crumb header
  crumb="$(get_crumb)"
  header="$(get_crumb_header)"
  local curl_args=(-sf "${auth_args[@]}" -H "Content-Type: application/x-www-form-urlencoded")
  if [[ -n "${crumb}" && -n "${header}" ]]; then
    curl_args+=(-H "${header}:${crumb}")
  fi
  curl "${curl_args[@]}" \
    --data-urlencode "script=${script}" \
    "${JENKINS_URL}/scriptText"
}

install_jdk_tool() {
  echo "Configuring JDK tool 'jdk-17' ..."
  run_script_console '
import jenkins.model.Jenkins
import hudson.model.JDK

def jenkins = Jenkins.instance
def toolName = "jdk-17"
def javaHome = System.getenv("JAVA_HOME")
if (javaHome == null || javaHome.isBlank()) {
  javaHome = new File(System.getProperty("java.home")).parentFile.absolutePath
}
def existing = jenkins.getJDK(toolName)
if (existing != null) {
  jenkins.removeJDK(existing)
}
jenkins.addJDK(new JDK(toolName, javaHome))
jenkins.save()
println("JDK " + toolName + " -> " + javaHome)
' >/dev/null
  echo "JDK tool configured."
}

install_ssh_credential() {
  if [[ ! -f "${STAGING_SSH_KEY_PATH}" ]]; then
    echo "SSH key not found at ${STAGING_SSH_KEY_PATH}; skipping credential install."
    return 0
  fi

  echo "Installing credential staging-ssh-key ..."
  local key_b64
  key_b64="$(base64 <"${STAGING_SSH_KEY_PATH}" | tr -d '\n')"
  local username="${STAGING_SSH_USER:-ubuntu}"

  run_script_console "
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import hudson.util.Secret
import jenkins.model.Jenkins

def jenkins = Jenkins.instance
def store = jenkins.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
def id = 'staging-ssh-key'

store.getCredentials(Domain.global()).findAll { it.id == id }.each { store.removeCredentials(Domain.global(), it) }

def keyBytes = '${key_b64}'.decodeBase64()
def privateKey = new String(keyBytes, 'UTF-8')
def keySource = new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(privateKey)
def cred = new BasicSSHUserPrivateKey(
  CredentialsScope.GLOBAL,
  id,
  '${username}',
  keySource,
  'App EC2 deploy key',
  'staging-ssh-key'
)
store.addCredentials(Domain.global(), cred)
jenkins.save()
println("Credential " + id + " installed.")
" >/dev/null
  echo "SSH credential configured."
}

create_seed_job() {
  echo "Creating/updating seed-job ..."
  local template="${ROOT_DIR}/jenkins/job-configs/seed-job.xml"
  local rendered
  rendered="$(mktemp)"

  if [[ ! -f "${template}" ]]; then
    echo "Missing ${template}"
    exit 1
  fi

  export GIT_REPO_URL="${GIT_REPO_URL:-https://github.com/CODEX-CRAFTSMAN-LABS/enterprise-rag.git}"
  export DEV_BRANCH="${DEV_BRANCH:-develop}"
  export PROD_BRANCH="${PROD_BRANCH:-main}"
  export AWS_REGION="${AWS_REGION:-ap-south-1}"
  export AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-440977419877}"
  export ECR_REGISTRY="${ECR_REGISTRY:-${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com}"
  export STAGING_SSH_HOST="${STAGING_SSH_HOST:-}"
  export STAGING_SSH_USER="${STAGING_SSH_USER:-ubuntu}"
  export STAGING_PATH="${STAGING_PATH:-/opt/enterprise-rag}"
  export DEPLOY_TARGET="${DEPLOY_TARGET:-ec2}"
  export ENABLE_DEPLOY="${ENABLE_DEPLOY:-true}"
  export ENABLE_SONAR="${ENABLE_SONAR:-false}"
  export OLLAMA_IMAGE="${OLLAMA_IMAGE:-alpine/ollama:0.23.2}"

  envsubst '${GIT_REPO_URL} ${DEV_BRANCH} ${PROD_BRANCH} ${AWS_REGION} ${AWS_ACCOUNT_ID} ${ECR_REGISTRY} ${STAGING_SSH_HOST} ${STAGING_SSH_USER} ${STAGING_PATH} ${DEPLOY_TARGET} ${ENABLE_DEPLOY} ${ENABLE_SONAR} ${OLLAMA_IMAGE}' \
    < "${template}" > "${rendered}"

  local crumb header
  crumb="$(get_crumb)"
  header="$(get_crumb_header)"
  local curl_args=(-sf "${auth_args[@]}" -H "Content-Type: application/xml")
  if [[ -n "${crumb}" && -n "${header}" ]]; then
    curl_args+=(-H "${header}:${crumb}")
  fi

  if curl -sf "${auth_args[@]}" "${JENKINS_URL}/job/seed-job/api/json" >/dev/null 2>&1; then
    curl "${curl_args[@]}" -X POST --data-binary @"${rendered}" \
      "${JENKINS_URL}/job/seed-job/config.xml"
  else
    curl "${curl_args[@]}" -X POST --data-binary @"${rendered}" \
      "${JENKINS_URL}/createItem?name=seed-job"
  fi

  rm -f "${rendered}"
  echo "seed-job created."
}

trigger_seed_job() {
  echo "Triggering seed-job build ..."
  local crumb header
  crumb="$(get_crumb)"
  header="$(get_crumb_header)"
  local curl_args=(-sf "${auth_args[@]}" -X POST)
  if [[ -n "${crumb}" && -n "${header}" ]]; then
    curl_args+=(-H "${header}:${crumb}")
  fi
  curl "${curl_args[@]}" \
    --data-urlencode "GIT_REPO_URL=${GIT_REPO_URL:-https://github.com/CODEX-CRAFTSMAN-LABS/enterprise-rag.git}" \
    --data-urlencode "DEV_BRANCH=${DEV_BRANCH:-develop}" \
    --data-urlencode "PROD_BRANCH=${PROD_BRANCH:-main}" \
    --data-urlencode "AWS_REGION=${AWS_REGION:-ap-south-1}" \
    --data-urlencode "AWS_ACCOUNT_ID=${AWS_ACCOUNT_ID:-440977419877}" \
    --data-urlencode "ECR_REGISTRY=${ECR_REGISTRY:-}" \
    --data-urlencode "STAGING_SSH_HOST=${STAGING_SSH_HOST:-}" \
    --data-urlencode "STAGING_SSH_USER=${STAGING_SSH_USER:-ubuntu}" \
    --data-urlencode "STAGING_PATH=${STAGING_PATH:-/opt/enterprise-rag}" \
    --data-urlencode "DEPLOY_TARGET=${DEPLOY_TARGET:-ec2}" \
    --data-urlencode "ENABLE_DEPLOY=${ENABLE_DEPLOY:-true}" \
    --data-urlencode "ENABLE_SONAR=${ENABLE_SONAR:-false}" \
    --data-urlencode "OLLAMA_IMAGE=${OLLAMA_IMAGE:-alpine/ollama:0.23.2}" \
    "${JENKINS_URL}/job/seed-job/buildWithParameters"
  echo "seed-job build queued."
}

wait_for_jenkins
install_jdk_tool
install_ssh_credential
create_seed_job

if [[ "${RUN_SEED_JOB}" == "true" ]]; then
  if [[ -x "${ROOT_DIR}/scripts/approve-jenkins-seed-script.sh" ]]; then
    "${ROOT_DIR}/scripts/approve-jenkins-seed-script.sh" || true
  fi
  trigger_seed_job
fi

echo ""
echo "Jenkins configure complete."
echo "  UI: ${JENKINS_URL}"
echo "  Jobs: build/ingestion-service, build/query-service"
echo "        deploy/dev/ingestion-service, deploy/dev/query-service"
echo ""
echo "Next: build/ingestion-service → deploy/dev/ingestion-service (same GIT_COMMIT_SHA), then query."
