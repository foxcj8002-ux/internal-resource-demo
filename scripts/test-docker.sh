#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:${HOST_PORT:-8080}}"
TRACE_ID="docker-test-$(date +%s)"
GATEWAY_IP="${TRUSTED_GATEWAY_IPS:-}"

request() { curl --fail --silent "$@"; }
wait_health() { for _ in $(seq 1 30); do request "$BASE_URL/actuator/health" | grep -q 'UP' && return 0; sleep 2; done; return 1; }

wait_health
if [ -z "$GATEWAY_IP" ]; then GATEWAY_IP=$(docker network inspect internal-resource-demo_default --format '{{(index .IPAM.Config 0).Gateway}}'); fi
request "$BASE_URL/actuator/health" | grep -q 'UP'
request -H "X-Trace-Id: $TRACE_ID-system" "$BASE_URL/api/system/info" | grep -q "$TRACE_ID-system"
test "$(request -H "X-Trace-Id: $TRACE_ID-files" "$BASE_URL/api/files" | grep -o '"resourceId":"internal-files"' | wc -l)" -ge 5
request -H "X-Trace-Id: $TRACE_ID-resource" "$BASE_URL/api/test-resources/read" | grep -q 'test-resource-read'
request -H "X-Trace-Id: $TRACE_ID-logs" "$BASE_URL/api/access-logs" | grep -q "$TRACE_ID-resource"

docker compose down
docker compose up -d
wait_health
request -H "X-Trace-Id: $TRACE_ID-direct-enabled" "$BASE_URL/api/test-resources/read" | grep -q '"success":true'

docker compose down
DIRECT_ACCESS_ENABLED=false TRUSTED_GATEWAY_IPS="$GATEWAY_IP" docker compose up -d
wait_health
status=$(curl --silent --output /tmp/direct-disabled.json --write-out '%{http_code}' -H "X-Trace-Id: $TRACE_ID-direct-disabled" "$BASE_URL/api/test-resources/read")
test "$status" = "403"
grep -q 'DIRECT_ACCESS_DISABLED' /tmp/direct-disabled.json
request -H "X-Trace-Id: $TRACE_ID-gateway" -H 'X-ZT-Gateway: zero-trust-rgw' "$BASE_URL/api/test-resources/read" | grep -q '"gatewayAccess":true'

echo 'Docker deployment checks passed.'
