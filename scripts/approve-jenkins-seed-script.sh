#!/usr/bin/env bash
# Approve pending Job DSL / Groovy scripts on Jenkins (fixes seed-job "script not yet approved").
# Run from laptop with jenkins/setup.env (JENKINS_URL, JENKINS_USER, JENKINS_TOKEN).
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

auth_args=()
if [[ -n "${JENKINS_TOKEN}" ]]; then
  auth_args=(-u "${JENKINS_USER}:${JENKINS_TOKEN}")
elif [[ -n "${JENKINS_PASSWORD}" ]]; then
  auth_args=(-u "${JENKINS_USER}:${JENKINS_PASSWORD}")
else
  echo "Set JENKINS_TOKEN or JENKINS_PASSWORD in jenkins/setup.env"
  exit 1
fi

crumb_json="$(curl -sf "${auth_args[@]}" "${JENKINS_URL}/crumbIssuer/api/json")"
crumb="$(echo "${crumb_json}" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumb'])")"
header="$(echo "${crumb_json}" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumbRequestField'])")"

read -r -d '' GROOVY <<'EOF' || true
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval
def sa = ScriptApproval.get()
def scriptHashes = sa.pendingScripts.collect { it.hash }
scriptHashes.each { sa.approveScript(it) }
def sigs = new ArrayList(sa.pendingSignatures)
sigs.each { sa.approveSignature(it) }
println "Approved ${scriptHashes.size()} script(s) and ${sigs.size()} signature(s). Pending scripts: ${sa.pendingScripts.size()}, signatures: ${sa.pendingSignatures.size()}."
EOF

result="$(curl -sf "${auth_args[@]}" -X POST -H "${header}:${crumb}" \
  --data-urlencode "script=${GROOVY}" \
  "${JENKINS_URL}/scriptText")"

echo "${result}"

if echo "${result}" | grep -q 'Pending scripts: 0'; then
  echo "Script approval OK. Re-run seed-job in Jenkins."
else
  echo "If items remain pending, open: ${JENKINS_URL}/scriptApproval/"
  exit 1
fi
