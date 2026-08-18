# GreenLoop AI 功能冒烟测试（PowerShell 版）
# 用法：
#   $env:TOKEN = "<普通用户或管理员的JWT>"
#   $env:ADMIN_TOKEN = "<管理员JWT，用于向量回填>"   # 可选，缺省复用 TOKEN
#   $env:BASE_URL = "http://localhost:8080"           # 可选
#   ./scripts/ai_smoke_test.ps1
#
# 判读：接口永远返回 200；AI 是否真实生效看内容是否与原文不同。
# 若返回内容=原文（publish/suggest）或 summary 为兜底文案（price/suggest），
# 说明 AI 调用失败被静默降级——优先检查 ai.timeout-ms 是否足够。

$BaseUrl = $env:BASE_URL
if ([string]::IsNullOrWhiteSpace($BaseUrl)) { $BaseUrl = "http://localhost:8080" }

$Token = $env:TOKEN
if ([string]::IsNullOrWhiteSpace($Token)) {
  Write-Host "Set TOKEN env var before running."
  exit 1
}
$AdminToken = $env:ADMIN_TOKEN
if ([string]::IsNullOrWhiteSpace($AdminToken)) { $AdminToken = $Token }

$headers = @{
  Authorization = "Bearer $Token"
  "Content-Type" = "application/json"
}
$adminHeaders = @{
  Authorization = "Bearer $AdminToken"
  "Content-Type" = "application/json"
}

$publishPayload = @{
  title = "九成新机械键盘"
  description = "樱桃轴，使用半年，功能正常，带原包装"
  price = 129.9
  categoryId = 2
  conditionLevel = 4
  coverImage = "/uploads/example.jpg"
  deliveryOptions = @("MEETUP")
} | ConvertTo-Json

Write-Host "`n[1/6] AI publish suggest (chat, 需 10~25s):"
Invoke-RestMethod -Uri "$BaseUrl/ai/publish/suggest" -Method Post -Headers $headers -Body $publishPayload

Write-Host "`n[2/6] AI price suggest (chat, 需 10~25s):"
Invoke-RestMethod -Uri "$BaseUrl/ai/price/suggest" -Method Post -Headers $headers -Body $publishPayload

Write-Host "`n[3/6] Risk check (纯规则引擎，不调 AI，秒回):"
Invoke-RestMethod -Uri "$BaseUrl/products/risk-check" -Method Post -Headers $headers -Body $publishPayload

Write-Host "`n[4/6] Semantic search (需先有向量数据):"
Invoke-RestMethod -Uri "$BaseUrl/products?keyword=键盘&searchMode=semantic&page=1&size=5" -Method Get

Write-Host "`n[5/6] Hybrid search (语义+词法加权):"
Invoke-RestMethod -Uri "$BaseUrl/products?keyword=键盘&searchMode=hybrid&page=1&size=5" -Method Get

Write-Host "`n[6/6] Embeddings rebuild (需 ADMIN Token; 存量商品向量回填):"
Invoke-RestMethod -Uri "$BaseUrl/ai/embeddings/rebuild?batchSize=200" -Method Post -Headers $adminHeaders

Write-Host "`n备注: recommendations 为公开接口，可另测 GET /products/{id}/recommendations"
Write-Host "注意: rebuild 后建议清理 Redis 陈旧推荐缓存: redis-cli --scan --pattern 'recommendations::*' | xargs redis-cli DEL"
