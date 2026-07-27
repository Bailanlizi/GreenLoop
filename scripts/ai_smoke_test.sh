#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:-}"

if [ -z "$TOKEN" ]; then
  echo "Set TOKEN env var before running."
  exit 1
fi

publish_payload='{"title":"九成新机械键盘","description":"樱桃轴，使用半年，功能正常，带原包装","price":129.9,"categoryId":2,"conditionLevel":4,"coverImage":"https://example.com/kb.jpg","deliveryOptions":["MEETUP"]}'

echo "AI publish suggest:"
curl -s -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
  -X POST "${BASE_URL}/ai/publish/suggest" -d "${publish_payload}"
echo

echo "Risk check:"
curl -s -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
  -X POST "${BASE_URL}/products/risk-check" -d "${publish_payload}"
echo

echo "Semantic search:"
curl -s "${BASE_URL}/products?keyword=机械键盘&searchMode=semantic"
echo
