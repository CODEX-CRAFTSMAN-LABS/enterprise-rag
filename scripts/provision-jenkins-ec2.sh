#!/usr/bin/env bash
set -euo pipefail

: "${AWS_REGION:?Set AWS_REGION to the AWS region.}"
: "${AMI_ID:?Set AMI_ID to an Ubuntu AMI ID.}"
: "${SUBNET_ID:?Set SUBNET_ID to the target subnet ID.}"
: "${SECURITY_GROUP_ID:?Set SECURITY_GROUP_ID to the Jenkins security group ID.}"
: "${KEY_NAME:?Set KEY_NAME to the EC2 key pair name.}"

INSTANCE_TYPE="${INSTANCE_TYPE:-t3.small}"
INSTANCE_NAME="${INSTANCE_NAME:-enterprise-rag-jenkins}"
ROOT_VOLUME_GB="${ROOT_VOLUME_GB:-30}"
IAM_INSTANCE_PROFILE="${IAM_INSTANCE_PROFILE:-}"

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
    --security-group-ids "${SECURITY_GROUP_ID}" \
    --key-name "${KEY_NAME}" \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${INSTANCE_NAME}}]" \
    --block-device-mappings "[{\"DeviceName\":\"/dev/sda1\",\"Ebs\":{\"VolumeSize\":${ROOT_VOLUME_GB},\"VolumeType\":\"gp3\",\"DeleteOnTermination\":true}}]" \
    "${profile_arg[@]}" \
    --query 'Instances[0].InstanceId' \
    --output text
)"

echo "Created Jenkins EC2 instance: ${instance_id}"
aws ec2 wait instance-running --region "${AWS_REGION}" --instance-ids "${instance_id}"

public_ip="$(
  aws ec2 describe-instances \
    --region "${AWS_REGION}" \
    --instance-ids "${instance_id}" \
    --query 'Reservations[0].Instances[0].PublicIpAddress' \
    --output text
)"

echo "Jenkins instance is running."
echo "Instance ID: ${instance_id}"
echo "Public IP: ${public_ip}"
echo ""
echo "Next steps:"
echo "  1. SSH in: ssh ubuntu@${public_ip}"
echo "  2. Clone this repository onto the Jenkins host."
echo "  3. Run ./scripts/bootstrap-jenkins-ubuntu.sh on that host."
