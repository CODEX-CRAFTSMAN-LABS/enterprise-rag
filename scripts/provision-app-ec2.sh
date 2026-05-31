#!/usr/bin/env bash
# Launch the app EC2 host (Postgres, Kafka, Redis, Ollama, Java services).
set -euo pipefail

export AWS_PAGER=""

: "${AWS_REGION:?Set AWS_REGION to the AWS region.}"
: "${AMI_ID:?Set AMI_ID to an Ubuntu AMI ID.}"
: "${SUBNET_ID:?Set SUBNET_ID to the target subnet ID.}"
: "${APP_SECURITY_GROUP_ID:?Set APP_SECURITY_GROUP_ID to the app security group ID.}"
: "${KEY_NAME:?Set KEY_NAME to the EC2 key pair name.}"

INSTANCE_TYPE="${APP_INSTANCE_TYPE:-t3.xlarge}"
INSTANCE_NAME="${APP_INSTANCE_NAME:-enterprise-rag-app}"
ROOT_VOLUME_GB="${APP_ROOT_VOLUME_GB:-80}"
IAM_INSTANCE_PROFILE="${APP_IAM_INSTANCE_PROFILE:-}"

profile_arg=()
if [[ -n "${IAM_INSTANCE_PROFILE}" ]]; then
  profile_arg=(--iam-instance-profile "Name=${IAM_INSTANCE_PROFILE}")
fi

instance_id="$(
  aws ec2 run-instances \
    --region "${AWS_REGION}" \
    --image-id "${AMI_ID}" \
    --instance-type "${INSTANCE_TYPE}" \
    --subnet-id "${SUBNET_ID}" \
    --security-group-ids "${APP_SECURITY_GROUP_ID}" \
    --key-name "${KEY_NAME}" \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${INSTANCE_NAME}}]" \
    --block-device-mappings "[{\"DeviceName\":\"/dev/sda1\",\"Ebs\":{\"VolumeSize\":${ROOT_VOLUME_GB},\"VolumeType\":\"gp3\",\"DeleteOnTermination\":true}}]" \
    "${profile_arg[@]}" \
    --query 'Instances[0].InstanceId' \
    --output text
)"

echo "Created app EC2 instance: ${instance_id}"
aws ec2 wait instance-running --region "${AWS_REGION}" --instance-ids "${instance_id}"

public_ip="$(
  aws ec2 describe-instances \
    --region "${AWS_REGION}" \
    --instance-ids "${instance_id}" \
    --query 'Reservations[0].Instances[0].PublicIpAddress' \
    --output text
)"

echo "App instance is running."
echo "Instance ID: ${instance_id}"
echo "Public IP: ${public_ip}"
echo ""
echo "Set in jenkins/setup.env:"
echo "  STAGING_SSH_HOST=${public_ip}"
echo ""
echo "Next steps:"
echo "  ssh ubuntu@${public_ip}"
echo "  git clone <repo> && cd enterprise-rag && ./scripts/bootstrap-ec2-ubuntu.sh"
