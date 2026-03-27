#!/usr/bin/env bash

set -euo pipefail

API_URL="${1:?Usage: ./benchmark.sh <API_URL>}"

echo "=========================================="
echo " Serverless Portal - Cold Start Benchmark"
echo "=========================================="
echo ""
echo "API: $API_URL"
echo ""

echo "=== Cold start tests (60s gap to force cold start) ==="
for i in 1 2 3; do
  echo "--- Cold Run $i ---"
  curl -s -w "\nTIME_TOTAL: %{time_total}s\n" \
    -X POST "$API_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"demo@test.com","password":"password"}'
  echo ""
  if [ "$i" -lt 3 ]; then
    echo "(waiting 60s for container to go cold...)"
    sleep 60
  fi
done

echo ""
echo "=== Warm start tests (back-to-back, 2s gap) ==="
for i in 1 2 3; do
  echo "--- Warm Run $i ---"
  curl -s -w "\nTIME_TOTAL: %{time_total}s\n" \
    -X POST "$API_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"demo@test.com","password":"password"}'
  echo ""
  sleep 2
done

echo ""
echo "Done. Copy the output above into docs/performance.md for your report."
