#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

step() { echo -e "\n========================================"; echo " $1"; echo "========================================"; }
ok()   { echo "  [OK] $1"; }
fail() { echo "  [FAIL] $1" >&2; exit 1; }

check_cmd() {
  if command -v "$1" &>/dev/null; then
    ok "$1 found"
  else
    fail "$1 is not installed. $2"
  fi
}

STACK_NAME="${STACK_NAME:-serverless-portal}"
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE="${AWS_PROFILE:-}"

SAM_CAPABILITIES="${SAM_CAPABILITIES:-CAPABILITY_IAM}"

aws_cmd=(aws --region "$AWS_REGION")
sam_cmd=(sam)
if [ -n "$AWS_PROFILE" ]; then
  aws_cmd+=(--profile "$AWS_PROFILE")
  sam_cmd+=(--profile "$AWS_PROFILE")
fi

step "Checking prerequisites"
check_cmd java   "Install JDK 21: https://adoptium.net/"
check_cmd node   "Install Node.js 18+: https://nodejs.org/"
check_cmd npm    "Comes with Node.js: https://nodejs.org/"
check_cmd aws    "Install AWS CLI v2: https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html"
check_cmd sam    "Install AWS SAM CLI: https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html"

step "Verifying AWS credentials"
"${aws_cmd[@]}" sts get-caller-identity >/dev/null
ok "AWS credentials OK (region: ${AWS_REGION})"

step "Installing frontend dependencies"
cd "$ROOT_DIR/frontend"
npm install
ok "Frontend dependencies installed"

step "Building auth-service"
cd "$ROOT_DIR/backend/auth-service"
chmod +x gradlew 2>/dev/null || true
./gradlew shadowJar --no-daemon -q
ok "auth-service JAR built"

step "Building content-service"
cd "$ROOT_DIR/backend/content-service"
if [ -f gradlew ]; then
  chmod +x gradlew 2>/dev/null || true
  ./gradlew shadowJar --no-daemon -q
else
  "$ROOT_DIR/backend/auth-service/gradlew" -p . shadowJar --no-daemon -q
fi
ok "content-service JAR built"

step "Preparing SAM template (using prebuilt JARs)"
cd "$ROOT_DIR/infrastructure"
"${sam_cmd[@]}" validate --template-file template.yaml >/dev/null
ok "SAM template validated"

step "Deploying to AWS (stack: ${STACK_NAME})"
"${sam_cmd[@]}" deploy \
  --template-file template.yaml \
  --stack-name "$STACK_NAME" \
  --region "$AWS_REGION" \
  --capabilities "$SAM_CAPABILITIES" \
  --resolve-s3 \
  --no-confirm-changeset \
  --no-fail-on-empty-changeset
ok "SAM deploy complete"

step "Resolving API endpoint"
API_URL="$("${aws_cmd[@]}" cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --query "Stacks[0].Outputs[?OutputKey=='ApiEndpoint'].OutputValue | [0]" \
  --output text)"

if [ -z "$API_URL" ] || [ "$API_URL" = "None" ]; then
  fail "Could not resolve ApiEndpoint output from stack '${STACK_NAME}'."
fi
ok "API endpoint: ${API_URL}"

step "Seeding demo user into AWS DynamoDB"
cd "$ROOT_DIR"
STACK_NAME="$STACK_NAME" AWS_REGION="$AWS_REGION" ${AWS_PROFILE:+AWS_PROFILE="$AWS_PROFILE"} bash scripts/seed-aws.sh
ok "Demo user seeded"

step "Writing frontend environment"
ENV_FILE="$ROOT_DIR/frontend/.env.local"
cat > "$ENV_FILE" <<EOF
VITE_API_URL=${API_URL}
EOF
ok "Wrote ${ENV_FILE}"

step "Setup complete!"
cat <<'SUMMARY'
  Next steps:

  1. Start the frontend dev server:
       cd frontend && npm run dev

  2. Open your browser:
       http://localhost:5173

  3. Log in with the demo account:
       Email:    demo@test.com
       Password: password
SUMMARY

