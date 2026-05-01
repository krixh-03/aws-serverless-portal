#!/usr/bin/env bash

set -euo pipefail

API_URL="${1:?Usage: ./benchmark.sh <API_URL>}"

echo "=========================================="
echo " Serverless Portal - Cold Start Benchmark"
echo "=========================================="
echo ""
echo "API: $API_URL"
echo ""
echo "IMPORTANT: For accurate cold start results, make sure the Lambda"
echo "has been idle for at least 15-20 minutes before running this script."
echo "Do NOT open the app or call the API beforehand."
echo ""

echo "=== Cold start test (1 request to a cold Lambda) ==="
echo "--- Cold Run ---"
curl -s -w "\nTIME_TOTAL: %{time_total}s\n" \
  -X POST "$API_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@test.com","password":"password"}'
echo ""

echo ""
echo "=== Warm start tests (3 back-to-back requests, 2s gap) ==="
for i in 1 2 3; do
  sleep 2
  echo "--- Warm Run $i ---"
  curl -s -w "\nTIME_TOTAL: %{time_total}s\n" \
    -X POST "$API_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"demo@test.com","password":"password"}'
  echo ""
done

echo ""
echo "Done. The cold run should show isColdStart:true with a high requestMs."
echo "The warm runs should show isColdStart:false with much lower requestMs."
echo "Copy the output above into docs/performance.md for your report."
