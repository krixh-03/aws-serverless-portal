#!/usr/bin/env bash

set -euo pipefail

TABLE_NAME="${TABLE_NAME:-PortalData}"

echo "Creating LocalStack DynamoDB table '${TABLE_NAME}'..."

aws --endpoint-url="http://localhost:4566" dynamodb create-table \
  --table-name "${TABLE_NAME}" \
  --attribute-definitions \
    AttributeName=PK,AttributeType=S \
    AttributeName=SK,AttributeType=S \
    AttributeName=GSI1PK,AttributeType=S \
    AttributeName=GSI1SK,AttributeType=S \
  --key-schema \
    AttributeName=PK,KeyType=HASH \
    AttributeName=SK,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --global-secondary-indexes \
    "IndexName=GSI1,KeySchema=[{AttributeName=GSI1PK,KeyType=HASH},{AttributeName=GSI1SK,KeyType=RANGE}],Projection={ProjectionType=ALL}"

echo "Table '${TABLE_NAME}' created in LocalStack."

