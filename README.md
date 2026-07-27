# GreenLoop - 校园二手交流平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.x-blue)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.x-purple)](https://element-plus.org/)
[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Node.js](https://img.shields.io/badge/Node.js-%3E=16.x-green)](https://nodejs.org/)

一个功能完善、技术栈现代化的全栈校园二手交易平台，采用前后端分离架构，集成 AI 智能搜索与推荐能力。

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

本项目是一个面向高校学生的二手物品在线交易社区，采用 **Monorepo** 结构统一管理三个独立子项目：

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

- JDK 21
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

### 步骤 3：配置后端

修改 `campus-trade-api/src/main/resources/application.yml`：

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

- **JWT 密钥**: 在 `application.yml` 的 `jwt.secret` 配置，生产环境请使用更长的随机密钥
- **密码加密**: 使用 BCryptPasswordEncoder，强度为 10

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