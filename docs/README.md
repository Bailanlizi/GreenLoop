# GreenLoop · 可信交易与资金清结算平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.x-blue)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.x-purple)](https://element-plus.org/)
[![Java 17](https://img.shields.io/badge/Java-17-orange)](https://adoptium.net/temurin/releases/?version=17)
[![Node.js](https://img.shields.io/badge/Node.js-20-green)](https://nodejs.org/)

> **技术栈：** Spring Boot · MySQL · Redis · MyBatis · Vue 3 · Vite
>
> **项目定位：** 面向个人与组织的可信资产交易平台，模拟银行核心清结算逻辑，实现交易创建、资金冻结、履约结算、退款回滚的全链路资金闭环。

一个面向个人、组织与园区场景的全栈可信交易平台，由校园二手交易场景演进而来，采用前后端分离架构。平台以**模拟银行核心清结算逻辑**为建设目标，实现从交易创建、资金冻结、履约结算到退款回滚的全链路资金闭环，并围绕一致性、幂等性、安全性、可审计性构建交易系统；当前已具备商品交易、配送、信用、运营管理及 AI 辅助能力。

---

## 目录

- [项目定位](#项目定位)
- [技术栈](#技术栈)
- [核心设计与实现](#核心设计与实现)
- [业务功能概览](#业务功能概览)
- [系统架构](#系统架构)
- [快速开始](#快速开始)
- [文档导航](#文档导航)
- [常见问题](#常见问题)

---

## 项目定位

GreenLoop 不是单纯的二手交易 Demo，而是以**金融科技系统**为建设目标的交易与清结算平台。它把银行核心系统的几条关键属性落到代码里：

- **资金全程不脱离平台可控范围**：买家付款先冻结、确认后结算、取消即原路退回。
- **每一笔资金变动都可追溯、不可篡改**：追加式流水记录变动前后余额快照。
- **重复请求不会重复扣款**：通过唯一键 + 行锁实现支付 / 退款幂等。
- **全链路在同一事务内完成**：核心资金操作强一致，乐观锁防并发超扣。

> 说明：本平台为**模拟**清结算，不接入真实支付渠道；但其账户模型、流水模型、幂等与一致性设计与真实银行核心账务思路一致，适合作为金融科技 / 支付方向的求职作品。

---

## 技术栈

| 分层 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7.18 · Spring Security · JWT · MyBatis · MySQL 8 · Redis / Spring Cache · Redisson · PageHelper |
| 前端 | Vue 3 · Vite · Vue Router · Pinia · Element Plus（管理端额外使用 ECharts） |
| 构建 | Maven（后端）· npm（前端） |

> 各技术的选型理由见《系统架构设计文档》第五章；版本与目录约定的实现基准见《开发基准》。

---

## 核心设计与实现

> 以下五点是本项目相对一般电商 Demo 的差异点，也是面向银行金融科技 / 支付岗位求职时应重点阐述的部分。

### 1. 虚拟账户体系
设计**可用余额 / 冻结余额双层结构**（`account` 表）。支付时资金先从「可用」转入「冻结」，确认收货后从「冻结」释放给卖家可用，取消时原路退回。资金始终在平台可控范围内，并通过 `version` 字段做乐观锁，防止并发超扣。

### 2. 清结算引擎
实现**充值 → 支付冻结 → 确认结算 → 退款回滚**全链路。所有资金操作（余额变动 + 流水记录）在**同一数据库事务内完成**，配合乐观锁版本号，保证「余额对得上、账不会乱」。系统总余额恒等于「总充值 − 总退款」（资金守恒）。

### 3. 幂等性控制
通过 `request_id` 数据库唯一约束与订单**行锁**实现支付 / 退款幂等：同一笔请求重复提交不会重复扣款或重复入账；一个订单仅对应一笔支付单，状态通过条件更新原子推进。

### 4. 不可变审计流水
资金流水（`account_flow`）采用**追加式记录，物理禁止修改和删除**，每笔变动都记录 `availableBefore/After`、`frozenBefore/After` 余额快照与业务单号，满足金融审计追溯要求，可完整回放任意账户的资金轨迹。

### 5. 订单状态机
严格定义**面交 / 快递双链路**状态流转（`PENDING_PAYMENT → AWAITING_MEETUP / AWAITING_SHIPMENT → SHIPPED → COMPLETED`，以及 `CANCELLED`）。通过条件更新原子锁定商品（下单即占库存），禁止非法状态回退与跨越，保证业务闭环可控。

### 6. 多实例任务与缓存防护
订单支付超时扫描通过 Redisson 分布式锁协调：多实例部署时同一周期仅一个实例扫描，实例异常后由 watch dog 超时释放并允许其他实例接管。Redis 缓存采用统一 `greenloop:` 前缀、按业务分级 TTL、TTL ±20% 随机抖动与空值短 TTL，分别降低缓存雪崩和缓存穿透风险。

### 7. 通知中心与可靠投递
系统通知独立于私信未读数，覆盖下单、支付、发货、结算、退款/取消与支付超时等交易节点。默认以本地事务写入通知；启用 `notifications.async-enabled` 后，交易事务会同库写入 Outbox，由 RabbitMQ 异步投递。消费者按来源事件幂等写入，发布失败重试三次后进入死信状态并可由管理员重放。

---

## 业务功能概览

### 用户端（`campus-trade-web`）
- **交易闭环**：商品浏览 / 搜索 / 发布、创建订单、选择面交或快递、确认收货、评价
- **资金管理**：账户余额、充值、支付、查看冻结 / 退款 / 结算与资金流水
- **互动**：私信沟通、商品收藏、系统通知、收货地址管理
- **AI 辅助**：发布文案润色、价格建议、语义搜索与「猜你喜欢」

### 管理后台（`campus-trade-admin`）
- 仪表盘数据可视化、用户 / 商品 / 订单 / 配送 / 交易地点管理
- **资金管理（只读）**：账户余额、充值单、支付单、冻结记录、退款单、结算单与资金流水查询（管理端不提供人工调账）

> 完整的接口路径、数据表结构、状态枚举与虚拟账户模型以《开发基准》为准。

---

## 系统架构

平台采用 **Monorepo** 结构，由三个独立子项目组成：

| 模块 | 职责 | 端口 |
|------|------|------|
| `campus-trade-api` | 后端服务：RESTful API、业务逻辑、数据持久化、安全认证、清结算引擎 | 8080 |
| `campus-trade-web` | 前端用户端：商品浏览、发布、交易、私信、资金管理等 | 5173 |
| `campus-trade-admin` | 管理后台：用户、商品、订单、配送、统计与资金流水只读管理 | 8000 |

后端当前为**模块化单体**（Spring Boot 单体应用承载用户、商品、订单、配送、消息、评价、文件、AI、虚拟账户与资金流水等模块），模块边界与未来微服务拆分方向见《系统架构设计文档》。数据库 ER 图与表关系也集中在该文档，避免分散维护。

---

## 快速开始

### 环境要求
- JDK 17 · Maven 3.6+ · Node.js 20（根目录 `.nvmrc`）· MySQL 8.x · Redis 6.x+

### 步骤 1：数据库初始化
```sql
CREATE DATABASE campus_trade CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_trade;
SOURCE campus-trade-api/src/main/resources/campus_trade.sql;
```
> `campus_trade.sql` 是**权威建表脚本**，已包含资金账户与流水相关表。根目录 `campus_trade_test.sql` 为测试库种子脚本，金额精度与资金表尚未同步，**禁止用它重建正式库**。

### 步骤 2：配置后端
共享配置（server/MyBatis/JWT/文件上传等）已内置在受控的 `application.properties`，无需手动配置。只需复制本地配置模板并填入**本机特有**的真实值（数据库账号、邮箱授权码、AI Key 等；该文件已被 `.gitignore` 忽略，不进仓库）：
```bash
cp campus-trade-api/src/main/resources/application_example.yml \
   campus-trade-api/src/main/resources/application.yml
```
或通过环境变量覆盖（优先级最高）：`SPRING_DATASOURCE_PASSWORD`、`SPRING_REDIS_PASSWORD`、`SPRING_MAIL_PASSWORD`、`JWT_SECRET`、`AI_API_KEY` 等。JWT 未配置时使用开发默认密钥，生产环境务必通过 `JWT_SECRET` 覆盖。



### 步骤 3：启动服务
```bash
cd campus-trade-api && mvn spring-boot:run      # 后端
cd campus-trade-web && npm install && npm run dev # 用户端
cd campus-trade-admin && npm install && npm run dev # 管理端
```

### Docker Compose（推荐用于完整环境）

Docker Desktop 启动后，复制环境变量模板并填写其中的强密码、JWT 密钥和邮件配置：

```powershell
Copy-Item .env.example .env
.\scripts\start-local.ps1 up
```

脚本会启动 MySQL、Redis、后端、用户端和管理端。首次创建的 MySQL 数据卷会以 UTF-8 自动导入完整建表脚本；已有业务库不要用此方式重建，应按增量脚本升级。常用命令：`.\scripts\start-local.ps1 logs`、`.\scripts\start-local.ps1 down`、`.\scripts\start-local.ps1 build`。

### 步骤 4：访问与初始管理员
| 服务 | 地址 |
|------|------|
| 后端 API | http://localhost:8080 |
| 用户端 | http://localhost:5173 |
| 管理后台 | http://localhost:8000/admin/ |

首次管理员由本地 `application.yml`（Docker 环境则为 `.env`）的 `security.bootstrap-admin` 配置创建。将 `enabled` 设为 `true` 并填写用户名、至少 12 位的强密码和邮箱；当同名用户不存在时，应用启动后只创建一次。创建成功后建议将 `enabled` 改回 `false`。

---

## 文档导航

为避免内容分散与重复，四类对外文档按职责划分，请按需求查阅：

| 文档 | 角色 | 主要内容 |
|------|------|----------|
| `README.md`（本文件） | 项目门面 | 定位、技术栈、核心设计亮点、快速开始、FAQ |
| `开发基准.md` | **唯一开发真源** | 接口路径、数据表、状态枚举、虚拟账户模型、安全 / 测试基线 |
| `系统架构设计文档.md` | 系统设计 | 设计原则、模块划分、ER 图、技术选型理由、演进方向 |
| `AI部署与自测.md` | AI 能力 | 向量 / 语义搜索 / 价格 / 文案的部署与自测 |

> 约定：接口、数据表、状态枚举、虚拟账户以《开发基准》为权威；ER 图与表关系以《系统架构设计文档》为权威；两处交叉引用，不再各自复述。

---

## 常见问题

### Q: 前端代理报错 `ECONNREFUSED`？
A: 后端服务未启动或端口配置错误。确保后端运行在 `http://localhost:8080`。

### Q: 邮件验证码无法发送？
A: 检查邮箱配置：确认 SMTP 已开启（QQ 邮箱需开启 SMTP 并生成授权码），尝试切换端口 587 (TLS) 或 465 (SSL)。

### Q: 图片无法显示？
A: 确保 `uploads` 目录存在且有写入权限，或检查 Vite 代理配置。

### Q: 管理员账号无法登录？
A: 检查 `security.bootstrap-admin` 是否已配置并启用。该机制不会覆盖已存在的同名用户，也不会重置密码；请通过管理端或数据库按既定运维流程修复账号。

---

欢迎提交 Issue 或 Pull Request 参与项目共建！
