#!/bin/bash

cat <<'EOB' >> /etc/ecs/ecs.config
ECS_CLUSTER=${cluster}
EOB

${install_prometheus_ebs}