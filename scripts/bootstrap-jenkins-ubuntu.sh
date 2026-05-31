#!/usr/bin/env bash
set -euo pipefail

if [[ "$(id -u)" -eq 0 ]]; then
  echo "Run this script as the target VM user, not root."
  exit 1
fi

sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg lsb-release software-properties-common

sudo install -m 0755 -d /etc/apt/keyrings

if [[ ! -f /etc/apt/keyrings/docker.gpg ]]; then
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
fi
sudo chmod a+r /etc/apt/keyrings/docker.gpg

. /etc/os-release
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null

if [[ ! -f /etc/apt/keyrings/jenkins-keyring.asc ]]; then
  curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key \
    | sudo tee /etc/apt/keyrings/jenkins-keyring.asc >/dev/null
fi
sudo chmod a+r /etc/apt/keyrings/jenkins-keyring.asc

echo \
  "deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" \
  | sudo tee /etc/apt/sources.list.d/jenkins.list >/dev/null

sudo apt-get update
sudo apt-get install -y \
  awscli \
  docker-ce \
  docker-ce-cli \
  containerd.io \
  docker-buildx-plugin \
  docker-compose-plugin \
  fontconfig \
  git \
  jq \
  jenkins \
  openjdk-17-jre-headless \
  openssh-client \
  unzip

sudo systemctl enable --now docker
sudo systemctl enable --now jenkins
sudo usermod -aG docker "$USER"
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins

docker --version
docker compose version
java -version
aws --version

echo ""
echo "Jenkins host bootstrap complete."
echo "Open port 8080 on the Jenkins EC2 security group to access the UI."
echo "Initial admin password:"
echo "  sudo cat /var/lib/jenkins/secrets/initialAdminPassword"
echo ""
echo "Recommended next steps:"
echo "  1. Attach an IAM role with ECR push permissions to this EC2 instance."
echo "  2. Add the app EC2 SSH key as the Jenkins credential 'staging-ssh-key'."
echo "  3. Configure the Jenkins JDK tool name as 'jdk-17'."
echo "  4. Run 'newgrp docker' before using docker without sudo in your current shell."
