#!/bin/bash

# Bash "strict mode"
set -euo pipefail

for f in ~{root,ec2-user}/.bashrc; do
  cat >> $f << 'EOB'
    export PS1='\n\[\e[91m[bastion-${ENV}]\e[0m \[\e[32m\]\u@\h: \[\e[33m\]\w\[\e[0m\]\n> '
    export LS_OPTIONS='--color=auto'
    alias ls='ls $LS_OPTIONS'
    alias ll='ls $LS_OPTIONS -l'
    alias la='ls $LS_OPTIONS -la'
    alias yy='sudo yum -y install'
EOB
done

cat >> /etc/ssh/sshd_config << 'EOB'
  Port 22
  Port 36017
EOB

# Echo commands before running them
set -x

systemctl restart sshd

yum -y update
yum -y install htop tree

amazon-linux-extras install -y docker
systemctl start docker

$(aws ecr get-login --no-include-email --region ap-southeast-2)

docker run \
  -d \
  --restart unless-stopped \
  -p 8000:80 \
  -e DNS_TTL=10s \
  -e SHIPREQ_ENV=${ENV_NAME} \
  -e SHIPREQ_URL=TODO \
  -e SHIPREQ_URL_HTTP=TODO \
  -e PROMETHEUS_URL=${PROMETHEUS_URL} \
  -e GRAFANA_URL=TODO \
  -e KIBANA_URL=TODO \
  -e JAEGER_URL= \
  --name portal \
  ${PORTAL_IMAGE}
