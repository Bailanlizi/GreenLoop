#!/usr/bin/env bash
# GreenLoop AI 功能冒烟测试（bash 版）
# 用法：
#   export TOKEN="<普通用户或管理员的JWT>"
#   export ADMIN_TOKEN="<管理员JWT>"   # 可选，缺省复用 TOKEN
#   export BASE_URL="http://localhost:8080"  # 可选
#   ./scripts/ai_smoke_test.sh
#
# 判读：接口永远返回 200；AI 是否真实生效看内容是否与原文不同。
# 若 publish/suggest 返回原文、price/suggest 返回兜底 summary，
# 说明 AI 调用被静默降级——优先检查 ai.timeout-ms 是否足够。
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:-}"
ADMIN_TOKEN="${ADMIN_TOKEN:-$TOKEN}"

if [ -z "$TOKEN" ]; then
  echo "Set TOKEN env var before running."
  exit 1
fi

publish_payload='{"title":"九成新机械键盘","description":"樱桃轴，使用半年，功能正常，带原包装","price":129.9,"categoryId":2,"conditionLevel":4,"coverImage":"/uploads/example.jpg","deliveryOptions":["MEETUP"]}'

echo "[1/6] AI publish suggest (chat, 需 10~25s):"
curl -s -m 60 -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
  -X POST "${BASE_URL}/ai/publish/suggest" -d "${publish_payload}"
echo

echo "[2/6] AI price suggest (chat, 需 10~25s):"
curl -s -m 60 -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
  -X POST "${BASE_URL}/ai/price/suggest" -d "${publish_payload}"
echo

echo "[3/6] Risk check (纯规则引擎，不调 AI，秒回):"
curl -s -m 15 -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
  -X POST "${BASE_URL}/products/risk-check" -d "${publish_payload}"
echo

echo "[4/6] Semantic search (需先有向量数据):"
curl -s -m 30 "${BASE_URL}/products?keyword=%E6%9C%BA%E6%A2%B0%E9%94%AE%E7%9B%98&searchMode=semantic&page=1&size=5"
echo

echo "[5/6] Hybrid search (语义+词法加权):"
curl -s -m 30 "${BASE_URL}/products?keyword=%E6%9C%BA%E6%A2%B0%E9%94%AE%E7%9B%98&searchMode=hybrid&page=1&size=5"
echo

echo "[6/6] Embeddings rebuild (需 ADMIN Token; 存量商品向量回填):"
curl -s -m 600 -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -X POST "${BASE_URL}/ai/embeddings/rebuild?batchSize=200"
echo

echo "备注: recommendations 为公开接口，可另测 GET /products/{id}/recommendations"
echo "注意: rebuild 后建议清理 Redis 陈旧推荐缓存:"
echo "  redis-cli --scan --pattern 'recommendations::*' | xargs redis-cli DEL"
