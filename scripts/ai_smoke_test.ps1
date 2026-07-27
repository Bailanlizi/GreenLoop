$BaseUrl = $env:BASE_URL
if ([string]::IsNullOrWhiteSpace($BaseUrl)) { $BaseUrl = "http://localhost:8080" }

$Token = $env:TOKEN
if ([string]::IsNullOrWhiteSpace($Token)) {
  Write-Host "Set TOKEN env var before running."
  exit 1
}

$headers = @{
  Authorization = "Bearer $Token"
  "Content-Type" = "application/json"
}

$publishPayload = @{
  title = "九成新机械键盘"
  description = "樱桃轴，使用半年，功能正常，带原包装"
  price = 129.9
  categoryId = 2
  conditionLevel = 4
  coverImage = "https://example.com/kb.jpg"
  deliveryOptions = @("MEETUP")
} | ConvertTo-Json

Write-Host "AI publish suggest:"
Invoke-RestMethod -Uri "$BaseUrl/ai/publish/suggest" -Method Post -Headers $headers -Body $publishPayload

Write-Host "Risk check:"
Invoke-RestMethod -Uri "$BaseUrl/products/risk-check" -Method Post -Headers $headers -Body $publishPayload

Write-Host "Semantic search:"
Invoke-RestMethod -Uri "$BaseUrl/products?keyword=机械键盘&searchMode=semantic" -Method Get
