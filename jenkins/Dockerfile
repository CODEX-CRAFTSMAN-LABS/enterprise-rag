FROM jenkins/jenkins:lts-jdk17

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends awscli ca-certificates curl docker.io git jq openssh-client unzip \
    && ARCH="$(uname -m)" \
    && case "$ARCH" in aarch64|arm64) KARCH=arm64 ;; *) KARCH=amd64 ;; esac \
    && KVER="$(curl -L -s https://dl.k8s.io/release/stable.txt)" \
    && curl -fsSL "https://dl.k8s.io/release/${KVER}/bin/linux/${KARCH}/kubectl" -o /usr/local/bin/kubectl \
    && chmod +x /usr/local/bin/kubectl \
    && rm -rf /var/lib/apt/lists/*

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

USER jenkins
