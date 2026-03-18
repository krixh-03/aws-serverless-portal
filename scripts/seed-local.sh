#!/usr/bin/env bash

set -euo pipefail

TABLE_NAME="${TABLE_NAME:-PortalData}"

EMAIL="demo@test.com"
PASSWORD_HASH='$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi' # bcrypt for "password"
USER_ID="demo-user-1"

echo "Seeding demo user into LocalStack DynamoDB table '${TABLE_NAME}'..."

aws --endpoint-url="http://localhost:4566" dynamodb put-item \
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

