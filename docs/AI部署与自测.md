# AI 功能部署与自测

> 本文按 2026-08-18 全链路实测结果编写（实测环境：DashScope qwen3.7-max + qwen3.7-text-embedding）。
> AI 模块出网点唯一：`AiClient`（OpenAI 兼容协议），任何兼容 OpenAI `/chat/completions` + `/embeddings` 的供应商均可接入。

## 0. 能力总览与架构事实

| 能力 | 接口 | 鉴权 | 依赖 AI | 说明 |
|---|---|---|---|---|
| 文案润色 | `POST /ai/publish/suggest` | 登录 | chat | 改写标题/描述，生成 highlights/tags |
| 价格建议 | `POST /ai/price/suggest` | 登录 | chat | 结合类目统计与同类样本给区间 |
| 语义搜索 | `GET /products?searchMode=semantic` | 公开 | embeddings | 对候选集做余弦相似度**重排序** |
| 混合搜索 | `GET /products?searchMode=hybrid` | 公开 | embeddings | 余弦 + 词法 Jaccard 加权 |
| 混合推荐 | `GET /products/{id}/recommendations` | 公开 | embeddings | 向量相似(0.7) + 协同过滤(0.3)，结果缓存 Redis |
| 商品风控 | `POST /products/risk-check` | 登录 | **否** | **纯规则引擎**（见下） |
| 向量回填 | `POST /ai/embeddings/rebuild` | **ADMIN** | embeddings | 存量商品批量补向量 |

务必了解的三个架构事实：

1. **语义/混合搜索是"重排序"而非"向量检索"**：候选集先由 SQL `LIKE keyword` 过滤，再对命中的商品按向量相似度排序。因此搜索词若在商品标题/描述中无字面命中，semantic 模式也返回空（实测：搜"交通代步"在无字面命中的库中返回空）。想命中需先用标准搜索可命中的关键词。
2. **risk-check 不调 AI**：`ProductRiskService` 是纯规则引擎——标题<4字、描述<20字、封面图与卖家近 30 天历史商品重复、标题 Jaccard 相似度≥0.85、价格偏离类目均值 3 倍/0.2 倍。发布商品时 MEDIUM/HIGH 风险会落入 `product_risks` 表，**不拦截交易**（实验性能力）。
3. **静默降级**：`ai.enabled=false`、api-key 为空、请求超时或供应商报错时，所有 AI 能力返回兜底结果（润色返回原文、价格建议返回类目统计区间、搜索退回默认排序），**接口仍是 200，服务照常运行**。判断 AI 是否真实生效：对比返回内容是否与原文不同。

## 1. 基本依赖

- JDK 17+、Maven、MySQL 8、Redis
- 一个 OpenAI 兼容供应商的 API Key（本项目实测用阿里云 DashScope 的 compatible-mode）

## 2. 配置（关键，配错会静默失效）

AI 配置位于本地 `application.yml`（被 `.gitignore` 忽略，参考 `application_example.yml` 模板），或通过环境变量注入：

```yaml
ai:
  enabled: true
  base-url: https://dashscope.aliyuncs.com/compatible-mode/v1   # 不配默认 api.openai.com/v1
  api-key: <你的Key>
  chat-model: qwen3.7-max                # 供应商可用模型名
  embedding-model: qwen3.7-text-embedding
  timeout-ms: 60000                      # 见下方"超时"说明
  temperature: 0.3
```

对应环境变量（Docker Compose 即此方式，注意 `AI_ENABLED` 默认 **false**）：`AI_ENABLED`、`AI_BASE_URL`、`AI_API_KEY`。

**超时必须留足（实测教训）**：qwen3.7-max 是推理模型（带思维链），业务级 prompt 实测耗时 9~21 秒。默认 `timeout-ms=10000` 会导致**大多数请求超时→静默降级**，现象是"接口正常返回但内容是原文"。建议 60000。embedding 模型响应约 1 秒，不受影响。

**只配 `AI_API_KEY` 不配 `AI_BASE_URL` 是常见错误**：请求会打到默认的 `api.openai.com`，DashScope 的 Key 直接静默失败，无任何报错。

## 3. 数据库增量（仅老库需要）

主 schema（`campus_trade_test.sql`）已包含这两张表；只有存量老库才需手动补建：

```sql
CREATE TABLE IF NOT EXISTS `product_embeddings` (
  `product_id` BIGINT NOT NULL,
  `embedding` MEDIUMTEXT NOT NULL,
  `model` VARCHAR(50) NOT NULL,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`),
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `product_risks` (
  `id` BIGINT AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `risk_level` VARCHAR(20) NOT NULL,
  `reasons` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_risk_product` (`product_id`),
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

向量以 JSON 文本存于 `product_embeddings.embedding`，余弦相似度在应用内存中计算（不在数据库做向量检索）。`product_risks` 仅存规则风控结果，属实验性能力，可仅建表不依赖。

## 4. 启动后端

```bash
# Linux/macOS
export AI_API_KEY="你的Key"
export AI_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
cd campus-trade-api && mvn spring-boot:run
```

```powershell
# Windows PowerShell
$env:AI_API_KEY = "你的Key"
$env:AI_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
cd campus-trade-api; mvn spring-boot:run
```

> 本机若已有 `application.yml`（含 ai 段）则无需环境变量。Docker 方式见根目录 `compose.yml` + `.env`（`AI_ENABLED` 默认 false，需显式开启）。

## 5. 向量回填（ADMIN 专属）

**新发布/更新的商品会自动调 embedding 接口写入向量**（实测验证），因此 rebuild 仅用于两类场景：接入 AI 前的存量商品、或切换了 embedding 模型后全量重建。

```bash
# 1. 用管理员账号登录拿 Token
curl -X POST http://localhost:8080/users/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"<管理员>","password":"<密码>"}'
# 2. 回填（batchSize 默认 200；逐商品调一次 embedding 接口，商品多时耗时线性增长）
curl -X POST "http://localhost:8080/ai/embeddings/rebuild?batchSize=200" \
  -H "Authorization: Bearer <ADMIN Token>"
# 返回 {"data":{"updated":N}}，N=成功写入的向量数
```

无 Token 调用返回 401，普通用户 Token 调用返回 403（`@PreAuthorize("hasRole('ADMIN')")`）。

**回填后务必清理推荐缓存**（重要运维步骤）：推荐结果按商品 ID 缓存于 Redis，`rebuild` 不会使其失效；且应用启动时的"清空缓存"实际只清本进程动态注册的缓存名，**不会清掉 Redis 中的历史 key**（实测踩坑：向量回填前缓存的空推荐结果会一直返回空）。清理命令：

```bash
redis-cli --scan --pattern "recommendations::*" | xargs redis-cli DEL
```

## 6. 核心接口自测（实测通过记录，2026-08-18）

```bash
# 登录拿 Token
POST /users/authenticate

# AI 文案润色（实测 11s 返回 AI 改写结果，含 highlights/tags）
POST /ai/publish/suggest
{"title":"出二手自行车","description":"骑了一年,9成新,刹车好使","price":300,"categoryId":6}

# AI 价格建议（实测 21s 返回区间+策略+3条tips；类目统计字段来自数据库，AI 失败时也在）
POST /ai/price/suggest
{"title":"出二手自行车","categoryId":6,"currentPrice":300,"conditionLevel":4}

# 规则风控（实测 1.2s；不调 AI）
POST /products/risk-check

# 语义/混合搜索（公开；实测对 LIKE 命中的候选正确重排序）
GET /products?keyword=收纳&searchMode=semantic
GET /products?keyword=收纳&searchMode=hybrid

# 混合推荐（公开；实测返回同类目+向量相似商品，缓存 Redis）
GET /products/{id}/recommendations
```

**快速判断 AI 是否生效**：`publish/suggest` 返回的 title 与原文相同 = 降级；`price/suggest` 的 `summary` 为"基于同类商品平均价给出参考区间"且 `tips` 为空 = 降级。

## 7. 自测脚本

```bash
export TOKEN="<登录Token>"
export ADMIN_TOKEN="<管理员Token>"   # 可选，缺省复用 TOKEN
./scripts/ai_smoke_test.sh           # bash 版
./scripts/ai_smoke_test.ps1          # PowerShell 版
```

脚本覆盖全部 6 组接口（两个 AI chat 接口、风控、两种搜索、rebuild），并在输出中标注预期耗时与降级判读方法。

## 8. 生产配置建议

- `ai.timeout-ms` 按供应商 P95 延迟的 2~3 倍设置；出现"返回原文"先怀疑超时
- 关闭调试日志：`logging.level.com.campus.trade.mapper: info`
- 独立域名 + HTTPS（Nginx 反向代理）；图片建议对象存储（非必须）
- 若不启用 AI：`ai.enabled=false` 即可，无需删除依赖，全部功能自动走兜底路径
