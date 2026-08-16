# AI功能部署与自测

## 1. 基本依赖
- JDK 21、Maven、MySQL、Redis（注：项目 `spring-boot-starter-parent` 为 2.7.18，官方仅支持到 Java 17，当前以 Java 21 运行，详见 `开发基准.md`）
- 已配置 `AI_API_KEY`（或在配置文件中设置 `ai.api-key`）

## 2. 生产配置建议（简版）
- 关闭调试日志：将 `logging.level.com.campus.trade.mapper` 调低为 `info`
- 使用独立域名 + HTTPS（Nginx 反向代理）
- 图片建议使用对象存储（非必须）

## 3. 数据库增量（已有库的情况）
如果你的数据库已经存在，只需要补充下面两张新表即可：
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

## 4. 启动后端
Linux/macOS:
```bash
export AI_API_KEY="你的Key"
cd campus-trade-platform/campus-trade-api
mvn spring-boot:run
```

Windows PowerShell:
```powershell
$env:AI_API_KEY="你的Key"
cd campus-trade-platform\campus-trade-api
mvn spring-boot:run
```

## 5. 向量回填（管理员接口）

发布一些商品后，执行一次全量向量构建：
```
POST /ai/embeddings/rebuild?batchSize=200
```

## 6. 核心接口自测（需要登录Token）
准备一个登录 Token：
```
POST /users/authenticate
```

然后测试：
```
POST /ai/publish/suggest
POST /products/risk-check
GET  /products?keyword=xxx&searchMode=semantic
GET  /products?keyword=xxx&searchMode=hybrid
GET  /products/{id}/recommendations
```

## 7. 自测脚本
你可以直接运行：
- `scripts/ai_smoke_test.sh`
- `scripts/ai_smoke_test.ps1`
