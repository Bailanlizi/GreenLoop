# GreenLoop - 可信交易与资金风控平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.x-blue)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.x-purple)](https://element-plus.org/)
[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Node.js](https://img.shields.io/badge/Node.js-%3E=16.x-green)](https://nodejs.org/)

一个面向个人、组织与园区场景的全栈可信交易平台，采用前后端分离架构，当前已具备商品交易、配送、信用、运营管理及 AI 辅助能力，并将持续演进模拟支付、资金托管、账户流水、退款结算与风险控制能力。

---

## 目录

- [项目概述](#项目概述)
- [技术栈](#技术栈)
- [项目架构](#项目架构)
- [核心功能](#核心功能)
- [AI 智能特性](#ai-智能特性)
- [快速开始](#快速开始)
- [API 模块概览](#api-模块概览)
- [配置说明](#配置说明)
- [常见问题](#常见问题)

---

## 项目概述

本项目采用 **Monorepo** 结构统一管理三个独立子项目。现阶段以闲置资产交易为核心业务场景，不再将用户身份限制为高校学生。

| 模块 | 职责 | 端口 |
|------|------|------|
| `campus-trade-api` | 后端服务，提供 RESTful API、业务逻辑、数据持久化、安全认证 | 8080 |
| `campus-trade-web` | 前端用户端，为普通用户提供商品浏览、发布、交易、私信等功能 | 5173 |
| `campus-trade-admin` | 管理后台，为管理员提供用户、商品、订单等数据的统一管理 | 8000 |

---

## 技术栈

### 后端 (`campus-trade-api`)

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | 应用框架 |
| Spring Security | 5.7.x | 认证与授权（RBAC） |
| JWT (jjwt) | 0.9.1 | 无状态身份令牌 |
| MyBatis | 2.2.2 | 持久层框架 |
| MySQL | 8.x | 主数据库 |
| Redis | 6.x+ | 缓存中间件 |
| PageHelper | 5.x | MyBatis 分页插件 |

### 前端 (`campus-trade-web` / `campus-trade-admin`)

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.x | 前端框架（Composition API） |
| Vite | 5.x | 构建工具 |
| Vue Router | 4.x | 路由管理 |
| Pinia | 2.x | 状态管理 |
| Element Plus | 2.x | UI 组件库 |
| ECharts | 5.x | 数据可视化（管理后台） |

---

## 项目架构

```
campus-trade-platform/
├── campus-trade-api/                    # 后端服务
│   ├── src/main/java/com/campus/trade/
│   │   ├── controller/                  # REST API 控制器
│   │   │   ├── ProductController.java   # 商品管理接口
│   │   │   ├── OrderController.java     # 订单管理接口
│   │   │   ├── UserController.java      # 用户管理接口
│   │   │   ├── MessageController.java   # 私信系统接口
│   │   │   ├── AdminController.java     # 管理员接口
│   │   │   ├── EmailController.java     # 邮箱验证码接口
│   │   │   ├── AiController.java        # AI 功能接口
│   │   │   └── ...
│   │   ├── service/                     # 业务逻辑层
│   │   │   ├── impl/                    # 服务实现类
│   │   │   └── ai/                      # AI 相关服务
│   │   │       ├── AiClient.java        # AI API 客户端
│   │   │       ├── AiEmbeddingService.java
│   │   │       ├── AiPriceSuggestionService.java
│   │   │       └── AiPublishService.java
│   │   ├── mapper/                      # MyBatis Mapper 接口
│   │   ├── entity/                      # 数据库实体类
│   │   ├── dto/                         # 数据传输对象
│   │   ├── security/                    # 安全相关
│   │   │   ├── JwtUtil.java             # JWT 工具类
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── UserDetailsServiceImpl.java
│   │   ├── config/                      # 配置类
│   │   │   ├── SecurityConfig.java      # Spring Security 配置
│   │   │   ├── RedisConfig.java         # Redis 配置
│   │   │   ├── WebConfig.java           # Web 配置（CORS、文件上传）
│   │   │   └── CacheInitializer.java    # 缓存初始化、默认管理员创建
│   │   ├── exception/                   # 异常处理
│   │   └── CampusTradeApiApplication.java  # 启动类
│   ├── src/main/resources/
│   │   ├── mapper/                      # MyBatis XML 映射文件
│   │   ├── application.yml              # 应用配置
│   │   └── campus_trade.sql             # 数据库初始化脚本
│   └── uploads/                         # 文件上传目录
│
├── campus-trade-web/                    # 前端用户端
│   ├── src/
│   │   ├── api/                         # API 请求封装
│   │   ├── views/                       # 页面组件
│   │   │   ├── Home.vue                 # 首页商品流
│   │   │   ├── ProductDetail.vue        # 商品详情
│   │   │   ├── PublishProduct.vue       # 发布商品
│   │   │   ├── Login.vue / Register.vue # 用户认证
│   │   │   ├── messages/                # 私信系统
│   │   │   └── ...
│   │   ├── components/                  # 公共组件
│   │   ├── router/                      # 路由配置
│   │   ├── stores/                      # Pinia 状态管理
│   │   └── utils/                       # 工具函数
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
│
└── campus-trade-admin/                  # 管理后台
    ├── src/
    │   ├── views/
    │   │   ├── dashboard/               # 数据仪表盘
    │   │   ├── user/                    # 用户管理
    │   │   ├── product/                 # 商品管理
    │   │   ├── order/                   # 订单管理
    │   │   ├── delivery/                # 配送管理
    │   │   └── location/                # 交易地点管理
    │   └── ...
    └── ...
```

---

## 核心功能

### 用户端 (`campus-trade-web`)

- **用户认证**：注册（邮箱验证码）、登录（JWT）、个人信息管理
- **商品管理**：浏览商品流、分类筛选、价格区间筛选、多维度排序
- **商品发布**：多图上传（最多3张）、表单校验、AI 润色标题/描述
- **订单系统**：创建订单、查看买卖订单、订单状态更新、收货评价
- **私信系统**：用户间私信沟通、会话列表、聊天窗口
- **收藏功能**：收藏/取消收藏、收藏列表管理
- **通知中心**：系统通知、订单状态变更推送

### 管理后台 (`campus-trade-admin`)

- **仪表盘**：平台核心数据可视化、用户增长趋势图表
- **用户管理**：用户列表、搜索、禁用/启用、编辑、重置密码、信誉分管理
- **商品管理**：商品列表、搜索、强制下架、编辑、删除
- **订单管理**：订单列表、多条件筛选、发货操作、状态管理、数据导出
- **配送管理**：批量发货、快递公司管理、运单号录入、订单导出 Excel/CSV
- **交易地点管理**：增删改查官方推荐的线下交易地点
- **资金管理（只读）**：账户余额、充值单、支付单、冻结记录、退款单、结算单与资金流水查询（管理端不提供人工调账）

---

## AI 智能特性

项目集成了 AI 能力，通过配置可开启以下功能：

| 功能 | 说明 | 配置项 |
|------|------|--------|
| **语义搜索** | 基于 Embedding 的模糊搜索，提升召回效果 | `search.hybrid` |
| **混合推荐** | 协同过滤 + Embedding 相似度的"猜你喜欢" | `recommend.hybrid` |
| **AI 价格建议** | 根据商品描述智能推荐合理价格区间 | `AiPriceSuggestionService` |
| **AI 一键润色** | 自动优化商品标题和描述文案 | `AiPublishService` |

---

## 快速开始

### 环境要求

- JDK 21（注：项目 `java.version` 配置为 21，但 `spring-boot-starter-parent` 为 2.7.18，官方仅支持到 Java 17。当前可在 21 上运行；后续计划统一为「Spring Boot 3.2+ + Java 21」或收敛到 Java 17，详见 `开发基准.md`）
- Maven 3.6+
- Node.js >= 16.x
- MySQL 8.x
- Redis 6.x+

### 步骤 1：克隆项目

```bash
git clone <repository-url>
cd campus-trade-platform
```

### 步骤 2：数据库初始化

```sql
CREATE DATABASE campus_trade CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_trade;
SOURCE campus-trade-api/src/main/resources/campus_trade.sql;
```

> 说明：`campus_trade.sql` 是**权威建表脚本**，已包含资金账户与流水相关表（`account`、`account_flow`、`recharge_order`、`payment_order`、`account_freeze_record`、`refund_order`、`settlement_order`）。项目根目录的 `campus_trade_test.sql` 为测试库种子脚本，当前尚未同步资金表，请勿用它重建正式库。

### 步骤 3：配置后端

推荐做法（避免泄露真实密钥）：

1. 复制示例配置：`cp campus-trade-api/src/main/resources/application_example.yml campus-trade-api/src/main/resources/application-local.yml`
2. 在 `application-local.yml` 中填入本地的数据库密码、Redis 密码、邮箱授权码、JWT 密钥与 AI Key（该文件已被 `.gitignore` 忽略，不会进入仓库）。
3. 也可通过环境变量覆盖：`SPRING_DATASOURCE_PASSWORD`、`SPRING_REDIS_PASSWORD`、`SPRING_MAIL_PASSWORD`、`JWT_SECRET`、`AI_API_KEY` 等。

> 注意：默认 `application.yml` 仅用于提交通用配置，**不要在其中写入真实密码 / 授权码 / JWT 密钥**。当前示例文件命名为 `application_example.yml`（下划线），并非 Spring Profile 自动加载命名；本地覆盖请使用 `application-local.yml`。

最小可用配置示例（`application-local.yml`）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_trade?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: <your-db-username>
    password: <your-db-password>
  redis:
    host: localhost
    port: 6379
    password: <your-redis-password-if-any>
  mail:
    host: smtp.qq.com
    username: <your-email>
    password: <your-email-authorization-code>

ai:
  enabled: true
  api-key: <your-ai-api-key>
```

### 步骤 4：启动服务

```bash
# 启动后端（新终端）
cd campus-trade-api
mvn spring-boot:run

# 启动前端用户端（新终端）
cd campus-trade-web
npm install
npm run dev

# 启动管理后台（新终端）
cd campus-trade-admin
npm install
npm run dev
```

### 步骤 5：访问服务

| 服务 | 地址 |
|------|------|
| 后端 API | http://localhost:8080 |
| 用户端 | http://localhost:5173 |
| 管理后台 | http://localhost:8000 |

### 管理员账号

系统启动时自动创建默认管理员账号：
- **用户名**: `admin`
- **密码**: `admin123`

---

## API 模块概览

后端控制器按业务领域组织：

| 控制器 | 路径前缀 | 功能 |
|--------|----------|------|
| `UserController` | `/users` | 用户注册、登录、认证 |
| `ProductController` | `/products` | 商品CRUD、搜索、推荐 |
| `OrderController` | `/orders` | 订单创建、状态更新 |
| `MessageController` | `/messages` | 私信会话、消息发送 |
| `FavoriteController` | `/favorites` | 收藏管理 |
| `RatingController` | `/ratings` | 评价管理 |
| `CategoryController` | `/categories` | 分类管理 |
| `EmailController` | `/email` | 验证码发送 |
| `AiController` | `/ai` | AI 功能（价格建议、文案润色） |
| `AdminController` | `/admin/users` | 管理员用户管理 |
| `AdminProductController` | `/admin/products` | 管理员商品管理 |
| `AdminOrderController` | `/admin/orders` | 管理员订单管理 |
| `AdminDeliveryController` | `/admin/delivery` | 配送管理 |
| `AdminDashboardController` | `/admin/dashboard` | 统计数据 |

---

## 配置说明

### 安全配置

- **JWT 密钥**: 在 `jwt.secret` 配置，生产环境请使用更长的随机密钥（当前使用 jjwt `0.9.1`，计划升级至 `0.11.x` 并引入密钥轮换）。
- **密码加密**: 使用 BCryptPasswordEncoder，强度为 10。
- **CORS**: 当前默认配置为允许任意来源并携带凭据（`allowCredentials(true)` + 通配来源），属于待收紧的安全项；生产环境应改为明确的前端白名单（如 `http://localhost:5173`、`http://localhost:8000`），该收紧工作已在阶段四（P0）规划中。
- **接口脱敏与字段加密**: 现阶段 phone / email 等敏感字段以明文存储与返回，AES 字段加密与接口脱敏列为阶段四（P4）安全增强项，尚未实现。
- **操作审计**: 后台关键操作（强制取消订单、账户冻结/解冻、权限变更）的追加式审计日志（`operation_audit_log`）列为阶段四（P1）规划，尚未实现。

### 测试与质量

- 后端测试基于 JUnit 5 + Mockito + Spring Boot Test（含 MockMvc 鉴权/越权回归测试）。
- 当前测试类 7 个、用例 25 个；覆盖订单状态机、支付幂等、余额不足、冻结/退款/结算、追加式流水快照、越权访问等。
- 待补强：真实数据库集成测试、多线程并发/乐观锁验证、资金守恒全局断言（系统总余额 = 总充值 − 总退款）。阶段四目标：`mvn test` 用例数 ≥ 30、通过率 100%。

### 文件上传

- 上传目录: `uploads/`（相对于项目根目录）
- 访问路径: `/uploads/{filename}`
- 单文件最大: 10MB

### 缓存策略

- 使用 Redis + Spring Cache
- 热点数据（如分类列表）自动缓存
- 应用启动时自动清空缓存

---

## 常见问题

### Q: 前端代理报错 `ECONNREFUSED`？

A: 后端服务未启动或端口配置错误。确保后端运行在 `http://localhost:8080`。

### Q: 邮件验证码无法发送？

A: 检查 `application.yml` 中的邮箱配置：
- 确认 SMTP 服务已开启（QQ邮箱需开启 SMTP 并生成授权码）
- 尝试切换端口：587 (TLS) 或 465 (SSL)

### Q: 图片无法显示？

A: 确保 `uploads` 目录存在且有写入权限，或检查 Vite 代理配置。

### Q: 管理员账号无法登录？

A: 系统启动时自动创建 `admin/admin123`。若数据库中已有 admin 用户，请先删除再重启。

---

## 开发规范

- 后端遵循 Spring Boot 最佳实践，使用分层架构
- 前端使用 Vue 3 Composition API，组件化开发
- API 接口返回统一格式：`{ "code": 200, "message": "success", "data": {} }`
- 所有数据库操作通过 MyBatis XML 管理，禁止硬编码 SQL

---

欢迎提交 Issue 或 Pull Request 参与项目共建！
