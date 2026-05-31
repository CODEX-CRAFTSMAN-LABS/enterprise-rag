#!/usr/bin/env bash
# Install Jenkins plugins from jenkins/plugins.txt on Ubuntu Jenkins (deb package).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PLUGIN_FILE="${ROOT_DIR}/jenkins/plugins.txt"
PLUGIN_CLI_VERSION="${PLUGIN_CLI_VERSION:-2.13.2}"
JENKINS_WAR="${JENKINS_WAR:-/usr/share/java/jenkins.war}"

if [[ ! -f "${PLUGIN_FILE}" ]]; then
  echo "Plugin file not found: ${PLUGIN_FILE}"
  exit 1
fi

if [[ ! -f "${JENKINS_WAR}" ]]; then
  echo "Jenkins WAR not found at ${JENKINS_WAR}. Is Jenkins installed?"
  exit 1
fi

tmp_jar="$(mktemp /tmp/jenkins-plugin-manager.XXXXXX.jar)"
cleanup() { rm -f "${tmp_jar}"; }
trap cleanup EXIT

curl -fsSL \
  -o "${tmp_jar}" \
  "https://github.com/jenkinsci/plugin-installation-manager-tool/releases/download/${PLUGIN_CLI_VERSION}/jenkins-plugin-manager-${PLUGIN_CLI_VERSION}.jar"

echo "Installing plugins from ${PLUGIN_FILE} ..."
sudo java -jar "${tmp_jar}" \
  --war "${JENKINS_WAR}" \
  --plugin-file "${PLUGIN_FILE}"

echo "Restarting Jenkins ..."
sudo systemctl restart jenkins
echo "Plugins installed. Wait ~60s for Jenkins to come back up."
