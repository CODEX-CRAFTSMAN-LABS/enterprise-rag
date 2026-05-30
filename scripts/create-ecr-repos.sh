#!/usr/bin/env bash
set -euo pipefail

: "${AWS_REGION:?Set AWS_REGION to the target AWS region.}"

ECR_IMAGE_RETENTION="${ECR_IMAGE_RETENTION:-50}"
INGESTION_REPOSITORY="${ECR_INGESTION_REPOSITORY:-enterprise-rag-ingestion-service}"
QUERY_REPOSITORY="${ECR_QUERY_REPOSITORY:-enterprise-rag-query-service}"

repositories=(
  "${INGESTION_REPOSITORY}"
  "${QUERY_REPOSITORY}"
)

create_lifecycle_policy() {
  local repo="$1"
  aws ecr put-lifecycle-policy \
    --region "${AWS_REGION}" \
    --repository-name "${repo}" \
    --lifecycle-policy-text "$(cat <<EOF
{
  "rules": [
    {
      "rulePriority": 1,
      "description": "Expire images beyond the most recent ${ECR_IMAGE_RETENTION}",
      "selection": {
        "tagStatus": "any",
        "countType": "imageCountMoreThan",
        "countNumber": ${ECR_IMAGE_RETENTION}
      },
      "action": {
        "type": "expire"
      }
    }
  ]
}
EOF
)"
}

for repo in "${repositories[@]}"; do
  if aws ecr describe-repositories --region "${AWS_REGION}" --repository-names "${repo}" >/dev/null 2>&1; then
    echo "ECR repository already exists: ${repo}"
  else
    aws ecr create-repository \
      --region "${AWS_REGION}" \
      --repository-name "${repo}" \
      --image-tag-mutability MUTABLE \
      --image-scanning-configuration scanOnPush=true >/dev/null
    echo "Created ECR repository: ${repo}"
  fi

  create_lifecycle_policy "${repo}" >/dev/null
done

aws ecr describe-repositories \
  --region "${AWS_REGION}" \
  --repository-names "${INGESTION_REPOSITORY}" "${QUERY_REPOSITORY}" \
  --query 'repositories[].repositoryUri' \
  --output text
