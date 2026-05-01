#!/usr/bin/env bash

set -euo pipefail

STACK_NAME="${STACK_NAME:-serverless-portal}"
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE="${AWS_PROFILE:-}"

TABLE_LOGICAL_ID="${TABLE_LOGICAL_ID:-PortalTable}"

EMAIL="${EMAIL:-demo@test.com}"
USER_ID="${USER_ID:-demo-user-1}"
_DEFAULT_BCRYPT='$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi' # bcrypt for "password"
PASSWORD_HASH="${PASSWORD_HASH:-$_DEFAULT_BCRYPT}"

aws_cmd=(aws --region "$AWS_REGION")
if [ -n "$AWS_PROFILE" ]; then
  aws_cmd+=(--profile "$AWS_PROFILE")
fi

echo "Resolving DynamoDB table from CloudFormation stack..."
TABLE_NAME="$("${aws_cmd[@]}" cloudformation describe-stack-resources \
  --stack-name "$STACK_NAME" \
  --logical-resource-id "$TABLE_LOGICAL_ID" \
  --query 'StackResources[0].PhysicalResourceId' \
  --output text)"

if [ -z "$TABLE_NAME" ] || [ "$TABLE_NAME" = "None" ]; then
  echo "Could not resolve table name for logical id '${TABLE_LOGICAL_ID}' in stack '${STACK_NAME}'." >&2
  exit 1
fi

echo "Seeding demo user into AWS DynamoDB table '${TABLE_NAME}' (region: ${AWS_REGION})..."

"${aws_cmd[@]}" dynamodb put-item \
  --table-name "${TABLE_NAME}" \
  --item "{
    \"PK\": {\"S\": \"USER#${USER_ID}\"},
    \"SK\": {\"S\": \"PROFILE#${USER_ID}\"},
    \"GSI1PK\": {\"S\": \"EMAIL#${EMAIL}\"},
    \"GSI1SK\": {\"S\": \"${EMAIL}\"},
    \"userId\": {\"S\": \"${USER_ID}\"},
    \"email\": {\"S\": \"${EMAIL}\"},
    \"passwordHash\": {\"S\": \"${PASSWORD_HASH}\"},
    \"role\": {\"S\": \"admin\"},
    \"createdAt\": {\"S\": \"2024-01-01T00:00:00Z\"}
  }"

echo "Seeded demo user:"
echo "  email:    ${EMAIL}"
echo "  password: password"
