#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
step() { echo -e "\n========================================"; echo " $1"; echo "========================================"; }
ok()   { echo "  [OK] $1"; }
fail() { echo "  [FAIL] $1" >&2; exit 1; }

check_cmd() {
  if command -v "$1" &>/dev/null; then
    ok "$1 found: $($1 --version 2>&1 | head -1)"
  else
    fail "$1 is not installed. $2"
  fi
}

# ---------------------------------------------------------------------------
# 1. Prerequisite checks
# ---------------------------------------------------------------------------
step "Checking prerequisites"

check_cmd java    "Install JDK 21: https://adoptium.net/"
check_cmd node    "Install Node.js 18+: https://nodejs.org/"
check_cmd npm     "Comes with Node.js: https://nodejs.org/"
check_cmd docker  "Install Docker Desktop: https://www.docker.com/products/docker-desktop/"
check_cmd aws     "Install AWS CLI v2: https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html"

JAVA_MAJOR=$(java -version 2>&1 | head -1 | sed 's/.*"\([0-9]*\)\..*/\1/')
if [ "$JAVA_MAJOR" -lt 21 ] 2>/dev/null; then
  echo "  [WARN] Java $JAVA_MAJOR detected. Java 21+ is recommended."
fi

# ---------------------------------------------------------------------------
# 2. Install frontend dependencies
# ---------------------------------------------------------------------------
step "Installing frontend dependencies"

cd "$ROOT_DIR/frontend"
npm install
ok "Frontend dependencies installed"

# ---------------------------------------------------------------------------
# 3. Build backend JARs
# ---------------------------------------------------------------------------
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

# ---------------------------------------------------------------------------
# 4. Start LocalStack
# ---------------------------------------------------------------------------
step "Starting LocalStack"

cd "$ROOT_DIR/infrastructure"

if docker ps --format '{{.Names}}' | grep -q '^localstack$'; then
  ok "LocalStack is already running"
else
  docker compose up -d
  ok "LocalStack container started"
fi

# ---------------------------------------------------------------------------
# 5. Wait for LocalStack to be ready
# ---------------------------------------------------------------------------
step "Waiting for LocalStack to be ready"

TIMEOUT=30
ELAPSED=0
until curl -sf http://localhost:4566/_localstack/health >/dev/null 2>&1; do
  if [ "$ELAPSED" -ge "$TIMEOUT" ]; then
    fail "LocalStack did not become ready within ${TIMEOUT}s. Check 'docker logs localstack'."
  fi
  sleep 2
  ELAPSED=$((ELAPSED + 2))
  echo "  Waiting... (${ELAPSED}s)"
done
ok "LocalStack is ready"

# ---------------------------------------------------------------------------
# 6. Create DynamoDB table
# ---------------------------------------------------------------------------
step "Creating DynamoDB table in LocalStack"

cd "$ROOT_DIR"
if aws --endpoint-url="http://localhost:4566" dynamodb describe-table --table-name PortalData >/dev/null 2>&1; then
  ok "Table 'PortalData' already exists — skipping"
else
  bash scripts/create-local-table.sh
  ok "Table created"
fi

# ---------------------------------------------------------------------------
# 7. Seed demo user
# ---------------------------------------------------------------------------
step "Seeding demo user"

bash scripts/seed-local.sh
ok "Demo user seeded"

# ---------------------------------------------------------------------------
# 8. Summary
# ---------------------------------------------------------------------------
step "Setup complete!"

cat <<'SUMMARY'
  Everything is ready. To start developing:

  1. Start the frontend dev server:
       cd frontend && npm run dev

  2. Open your browser:
       http://localhost:5173

  3. Log in with the demo account:
       Email:    demo@test.com
       Password: password

  LocalStack is running on http://localhost:4566
  To stop LocalStack:  cd infrastructure && docker compose down
SUMMARY
