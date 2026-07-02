# AmazonOps AI — Amazon 卖家智能运营平台

基于 [ruoyi-vue-pro (Yudao)](https://gitee.com/zhijiantianya/ruoyi-vue-pro) 框架构建的 **Amazon 卖家全链路智能运营平台**。涵盖选品调研、Listing 优化、订单管理、库存预测、广告自动化、评论分析、AI 决策支持等核心能力。

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 2.7.18 |
| JDK | OpenJDK / Temurin | **1.8** |
| ORM | MyBatis Plus | 3.5.16 |
| 数据库 | MySQL | 8.x |
| 缓存 | Redis + Redisson | 6.x / 4.6.1 |
| 定时任务 | Quartz (yudao-spring-boot-starter-job) | 集群模式, 25 线程 |
| AI 框架 | Spring AI (ChatClient + Function Calling) | 1.1.5 |
| SP-API 客户端 | OkHttp + AWS Signature V4 | 4.12.0 |
| 前端框架 | Vue 3 + Element Plus + ECharts | 3.2 |
| 构建 | Maven | 3.8+ |
| 容器化 | Docker + Docker Compose | - |

---

## 模块架构

```
amazonops-base (root)
│
├── yudao-module-amazon-common/              # 共享基础设施
│   ├── amazon-common-core/                  # SP-API 客户端、加密、限流、重试
│   ├── amazon-common-model/                 # 公共数据模型
│   ├── amazon-common-sync/                  # 同步框架: AbstractAmazonSyncJob
│   └── amazon-common-encrypt/               # 加密扩展
│
├── yudao-module-amazon-spapi/               # SP-API 集成层
│   ├── spapi-client/                        # 客户端配置
│   ├── spapi-auth/                          # 认证
│   └── spapi-ads/                           # 广告 API
│
├── yudao-module-amazon-shop/                # M1: 店铺管理 + API 监控 (25 files)
├── yudao-module-amazon-research/            # M2: 选品调研 + BSR分析 (41 files)
├── yudao-module-amazon-listing/             # M3: 商品刊登 + 自动定价 (35 files)
├── yudao-module-amazon-order/               # M4: 订单管理 + FBA发货 (31 files)
├── yudao-module-amazon-inventory/           # M5: 库存监控 + 预测 (31 files)
├── yudao-module-amazon-ad/                  # M6: 广告管理 + 规则引擎 (38 files)
├── yudao-module-amazon-review/              # M7: 评论管理 + 预警 (33 files)
├── yudao-module-amazon-report/              # M8: 运营看板 + AI助手 (27 files)
│
├── yudao-module-amazon-ai/                  # AI 能力层 (44 files)
│   ├── amazon-ai-core/                      # 模型路由、Prompt引擎、Token追踪
│   ├── amazon-ai-agent/                     # OpsAgent + 8个Function工具
│   ├── amazon-ai-listing/                   # AI Listing生成 + 诊断
│   ├── amazon-ai-review/                    # AI 评论情感分析
│   ├── amazon-ai-research/                  # AI 选品推荐
│   ├── amazon-ai-ad/                        # AI 广告优化
│   └── amazon-ai-inventory/                 # AI 库存预测
│
├── yudao-framework/                         # 框架基础 starters
├── yudao-dependencies/                      # Maven 依赖版本管理
└── yudao-server/                            # 主启动模块
```

---

## 项目统计

| 指标 | 数值 |
|------|------|
| Amazon 模块总数 | 11 (2 基础 + 8 业务 + 1 AI) |
| Java 文件 (main) | 356 |
| Java 文件 (test) | 19 |
| Controller | 24 |
| Service 对 | 38+ |
| 数据对象 (DO) | 27 |
| Mapper | 27 |
| SQL 表 | 26 |
| SQL DDL 行数 | 768 |
| Sync Job 实现 | 5 |
| AI Function 工具 | 8 |
| 测试方法总数 | 249 |
| Vue 前端页面 | 26 (8 模块, 34 文件含 API) |
| 整体项目完成度 | ~75% |

---

## 环境要求

| 依赖 | 最低版本 | 说明 |
|------|----------|------|
| JDK | **1.8** | 必须 JDK 8, 不兼容 JDK 11+ |
| Maven | 3.8+ | 构建工具 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 6.0+ | 缓存 + 分布式锁 + 消息队列 |
| Node.js | 16+ | 前端构建 (可选, 前端尚未集成) |
| Docker | 20+ | 容器化部署 (可选) |
| Docker Compose | 2.0+ | 一键部署 (可选) |

---

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url> amazonMall
cd amazonMall
```

### 2. 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE \`ruoyi-vue-pro-jdk8\` \
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入基座表结构 (yudao 框架)
mysql -u root -p ruoyi-vue-pro-jdk8 < sql/mysql/ruoyi-vue-pro.sql

# 导入 Amazon 业务表 (26 张表, 含种子数据)
mysql -u root -p ruoyi-vue-pro-jdk8 < sql/mysql/amazon.sql
```

### 3. 修改配置

编辑 `yudao-server/src/main/resources/application-local.yaml`，替换所有占位值:

```yaml
# ---- 数据库 ----
spring:
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:mysql://127.0.0.1:3306/ruoyi-vue-pro-jdk8?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true
          username: root
          password: YOUR_PASSWORD_HERE

# ---- Redis ----
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0

# ---- Amazon SP-API (替换为你的真实凭证) ----
amazon:
  sp-api:
    app-id: amzn1.application.xxx
    client-id: amzn1.application-oa2-client.xxx
    client-secret: YOUR_CLIENT_SECRET
    aws-access-key-id: YOUR_AWS_ACCESS_KEY_ID
    aws-secret-access-key: YOUR_AWS_SECRET_ACCESS_KEY
    aws-role-arn: arn:aws:iam::YOUR_ROLE_ARN
    redirect-uri: http://localhost:48080/admin-api/amazon/shop/callback
    default-marketplace-id: ATVPDKIKX0DER   # 美国站
    sandbox: true                            # 开发阶段设为 true
    encryption-key: BASE64_ENCODED_32BYTE_KEY  # AES-256, 加密存储凭证
  ai:
    openai:
      base-url: https://api.openai.com
      api-key: YOUR_OPENAI_API_KEY
      default-model: gpt-4o
    budget:
      monthly-limit-usd: 100.00
      per-request-limit-usd: 1.00
  sync:
    enabled: true
    default-delta-window: 60m
    max-errors-per-task: 100
    default-batch-size: 100
```

### 4. 将 Amazon 模块接入 yudao-server

当前 `yudao-server/pom.xml` 未声明 Amazon 依赖，需要手动添加:

```xml
<!-- 在 yudao-server/pom.xml 的 <dependencies> 中添加 -->
<dependency>
    <groupId>cn.iocoder.boot</groupId>
    <artifactId>yudao-module-amazon-shop</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>cn.iocoder.boot</groupId>
    <artifactId>yudao-module-amazon-research</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>cn.iocoder.boot</groupId>
    <artifactId>yudao-module-amazon-listing</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>cn.iocoder.boot</groupId>
    <artifactId>yudao-module-amazon-order</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>cn.iocoder.boot</groupId>
    <artifactId>yudao-module-amazon-inventory</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>cn.iocoder.boot</groupId>
    <artifactId>yudao-module-amazon-ad</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>cn.iocoder.boot</groupId>
    <artifactId>yudao-module-amazon-review</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>cn.iocoder.boot</groupId>
    <artifactId>yudao-module-amazon-report</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>cn.iocoder.boot</groupId>
    <artifactId>yudao-module-amazon-ai</artifactId>
    <version>${revision}</version>
</dependency>
```

### 5. 构建并启动

```bash
# 编译打包 (跳过测试加速)
mvn clean install -DskipTests

# 启动后端服务
cd yudao-server
mvn spring-boot:run

# 或直接用 jar
java -jar yudao-server/target/yudao-server.jar
```

启动成功后访问:

| 地址 | 说明 |
|------|------|
| `http://localhost:48080` | 后端 API |
| `http://localhost:48080/doc.html` | Swagger 接口文档 (Knife4j) |
| 默认管理员 | `admin / admin123` |

### 6. Docker Compose 一键部署 (可选)

```bash
cd script/docker
docker-compose up -d
```

包含 4 个服务:

| 服务 | 端口 | 说明 |
|------|------|------|
| mysql | 3306 | MySQL 8, 自动初始化 SQL |
| redis | 6379 | Redis 6 |
| server | 48080 | yudao-server 后端 |
| admin | 8080 | Vue Admin 前端 |

---

## 数据库表结构

共 **26 张** Amazon 业务表, 分属 8 个模块。所有表使用 `utf8mb4_unicode_ci`，ENGINE=InnoDB。

### M1: 店铺管理 (3 表)

**amazon_shop** — 店铺/卖家账号

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| name | varchar(128) | 店铺名称 |
| marketplace_id | varchar(32) | 站点 ID |
| seller_id | varchar(64) | 卖家 ID |
| status | tinyint | 状态 (0=禁用, 1=启用) |
| credentials | text | 加密存储的 SP-API 凭证 (JSON) |
| remark | varchar(512) | 备注 |

**amazon_marketplace** — 站点配置 (含 20 条种子数据: US/CA/MX/DE/ES/FR/IT/UK/NL/SE/PL/BE/TR/EG/AE/SA/JP/AU/IN/SG)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| marketplace_id | varchar(32) | Amazon Marketplace ID |
| country_code | varchar(4) | 国家代码 |
| name | varchar(64) | 站点名称 |
| currency | varchar(8) | 货币代码 |
| timezone | varchar(64) | 时区 |
| endpoint | varchar(128) | SP-API 端点 |

**amazon_api_log** — SP-API 调用审计日志

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺 ID |
| api_type | varchar(64) | API 类型 (Orders/Catalog/Inventory 等) |
| http_method | varchar(16) | HTTP 方法 |
| request_url | varchar(512) | 请求 URL |
| status_code | int | HTTP 状态码 |
| response_time | int | 响应时间 (ms) |
| error_message | text | 错误信息 |

### M2: 选品调研 (5 表)

**amazon_niche** — 品类/利基市场分析

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺 ID |
| category | varchar(256) | 品类名称 |
| marketplace_id | varchar(32) | 站点 ID |
| avg_price | decimal(10,2) | 平均价格 |
| monthly_sales | int | 月销量 |
| competition_level | varchar(16) | 竞争程度 (LOW/MEDIUM/HIGH) |
| opportunity_score | int | 机会评分 (0-100) |

**amazon_bsr_regression** — BSR-销量回归模型参数

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| category | varchar(256) | 品类 |
| marketplace_id | varchar(32) | 站点 |
| intercept | double | 回归截距 |
| slope | double | 回归斜率 |
| r_squared | double | R² 拟合度 |

**amazon_product_opportunity** — 选品机会跟踪

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| niche_id | bigint | 关联品类 |
| product_name | varchar(256) | 商品名称 |
| opportunity_score | int | 机会评分 |
| estimated_monthly_revenue | decimal(12,2) | 预估月收入 |
| recommendation | varchar(16) | 建议 (STRONG_BUY/BUY/HOLD/AVOID) |

**amazon_financial_projection** — 财务预测

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| product_opportunity_id | bigint | 关联选品 |
| startup_cost | decimal(12,2) | 启动成本 |
| break_even_months | int | 回本月数 |
| profit_12m | decimal(12,2) | 12 个月利润 |

**amazon_supplier_match** — 供应商匹配

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| product_opportunity_id | bigint | 关联选品 |
| supplier_name | varchar(256) | 供应商名称 |
| moq | int | 最低起订量 |
| unit_price | decimal(10,2) | 单价 |
| rating | decimal(3,2) | 评分 |

### M3: 商品刊登 (3 表)

**amazon_product** — 商品 Listing

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺 |
| asin | varchar(16) | ASIN |
| sku | varchar(64) | SKU |
| title | varchar(512) | 商品标题 |
| brand | varchar(128) | 品牌 |
| price | decimal(10,2) | 价格 |
| quantity | int | 库存 |
| bsr_rank | int | BSR 排名 |
| ai_score | int | AI 评分 |
| bullet_points | text | 五点描述 |
| description | text | 商品描述 |
| keywords | text | 搜索关键词 |
| image_url | varchar(512) | 主图 URL |

**amazon_listing_version** — Listing 版本历史

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| product_id | bigint | 关联商品 |
| version | int | 版本号 |
| title | varchar(512) | 标题 |
| change_summary | varchar(512) | 变更摘要 |

**amazon_auto_price_rule** — 自动定价规则

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| name | varchar(128) | 规则名称 |
| strategy_type | varchar(32) | 策略类型 (BUY_BOX/FIXED/DYNAMIC) |
| target_price | decimal(10,2) | 目标价格 |
| min_price | decimal(10,2) | 最低价 |
| max_price | decimal(10,2) | 最高价 |
| enabled | bit(1) | 是否启用 |

### M4: 订单管理 (3 表)

**amazon_order** — 亚马逊订单

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺 |
| amazon_order_id | varchar(32) | 亚马逊订单号 |
| order_status | varchar(32) | 订单状态 (Pending/Unshipped/Shipped/Canceled) |
| order_total | decimal(12,2) | 订单金额 |
| fulfillment_channel | varchar(8) | 配送渠道 (AFN=FBM, MFN=自发货) |
| purchase_date | datetime | 下单时间 |
| is_prime | bit(1) | 是否 Prime |

**amazon_order_item** — 订单行项目

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| order_id | bigint | 关联订单 |
| asin | varchar(16) | ASIN |
| sku | varchar(64) | SKU |
| quantity_ordered | int | 订购数量 |
| item_price | decimal(10,2) | 商品单价 |

**amazon_fba_shipment** — FBA 发货计划

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺 |
| shipment_id | varchar(64) | 发货编号 |
| destination_fulfillment_center_id | varchar(32) | FBA 仓库 |
| status | varchar(32) | 状态 (PENDING→SHIPPED→RECEIVED→COMPLETED) |
| total_units | int | 总件数 |

### M5: 库存管理 (3 表)

**amazon_inventory** — FBA 库存快照

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺 |
| asin | varchar(16) | ASIN |
| sku | varchar(64) | SKU |
| quantity_available | int | 可用库存 |
| quantity_reserved | int | 预留库存 |
| quantity_inbound_working | int | 入库中 |
| quantity_unfulfillable | int | 不可售 |

**amazon_inventory_forecast** — 库存预测

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| inventory_id | bigint | 关联库存 |
| forecast_date | date | 预测日期 |
| predicted_stock | int | 预测库存 |
| daily_sales | decimal(8,2) | 日均销量 |
| days_of_supply | int | 可售天数 |
| reorder_point | int | 补货点 |
| safety_stock | int | 安全库存 |

**amazon_replenish_alert** — 补货预警

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺 |
| sku | varchar(64) | SKU |
| current_stock | int | 当前库存 |
| reorder_point | int | 补货点 |
| suggested_quantity | int | 建议补货量 |
| lead_time_days | int | 备货周期 (天) |
| alert_type | varchar(16) | 预警类型 (LOW_STOCK/OUT_OF_STOCK/OVERSTOCK) |
| status | varchar(16) | 状态 (PENDING/CONFIRMED/CANCELLED) |

### M6: 广告管理 (4 表)

**amazon_ad_campaign** — 广告活动

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺 |
| campaign_name | varchar(256) | 活动名称 |
| campaign_type | varchar(16) | 类型 (SP/SB/SD) |
| state | varchar(16) | 状态 (ENABLED/PAUSED/ARCHIVED) |
| daily_budget | decimal(10,2) | 日预算 |

**amazon_ad_report_daily** — 日度广告报表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| campaign_id | bigint | 广告活动 |
| report_date | date | 日期 |
| impressions | int | 展示量 |
| clicks | int | 点击量 |
| spend | decimal(10,2) | 花费 |
| sales | decimal(12,2) | 销售额 |
| acos | decimal(8,4) | ACoS |
| roas | decimal(8,4) | ROAS |
| cpc | decimal(8,4) | CPC |
| ctr | decimal(8,4) | CTR |

**amazon_ad_search_term** — 搜索词报告

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| campaign_id | bigint | 广告活动 |
| search_term | varchar(256) | 搜索词 |
| impressions | int | 展示量 |
| clicks | int | 点击量 |
| ai_tag | varchar(16) | AI 标签 (OPPORTUNITY/WASTE/KEEP/NEGATIVE) |

**amazon_ad_rule** — 广告自动化规则

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| name | varchar(128) | 规则名称 |
| scope | varchar(16) | 作用域 (CAMPAIGN/ADGROUP/KEYWORD) |
| condition_json | text | 条件 JSON |
| action_json | text | 动作 JSON |
| enabled | bit(1) | 是否启用 |

### M7: 评论管理 (3 表)

**amazon_review** — 客户评论

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺 |
| asin | varchar(16) | ASIN |
| review_id | varchar(64) | 评论 ID |
| star_rating | int | 星级 (1-5) |
| title | varchar(256) | 标题 |
| body | text | 内容 |
| sentiment | varchar(16) | AI 情感 (POSITIVE/NEGATIVE/NEUTRAL) |
| ai_topics | text | AI 提取的主题 (JSON) |
| verified_purchase | bit(1) | 是否已验证购买 |

**amazon_review_alert** — 评论预警规则

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| name | varchar(128) | 预警名称 |
| star_threshold | int | 星级阈值 |
| notification_method | varchar(32) | 通知方式 |
| enabled | bit(1) | 是否启用 |

**amazon_customer_template** — 客服消息模板

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| name | varchar(128) | 模板名称 |
| type | varchar(32) | 类型 (REFUND/EXCHANGE/GUIDE/THANKS/APOLOGY) |
| content | text | 模板内容 |
| variables | varchar(512) | 变量列表 |

### M8: 报表 + AI (3 表)

**amazon_dashboard_metric** — 运营看板日指标

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺 |
| metric_date | date | 日期 |
| total_sales | decimal(12,2) | 总销售额 |
| total_orders | int | 总订单数 |
| ad_spend | decimal(10,2) | 广告花费 |
| profit | decimal(12,2) | 利润 |
| inventory_health_score | int | 库存健康分 |

**amazon_weekly_report** — AI 周报

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺 |
| period_start | date | 周期开始 |
| period_end | date | 周期结束 |
| summary | text | AI 生成的摘要 |
| recommendations | text | AI 建议 |
| status | varchar(16) | 状态 (GENERATED/PENDING/FAILED) |

**amazon_ai_token_usage** — AI Token 用量追踪

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺 |
| task_type | varchar(32) | 任务类型 |
| model | varchar(64) | 模型名称 |
| prompt_tokens | int | Prompt Token 数 |
| completion_tokens | int | 完成 Token 数 |
| cost_usd | decimal(10,6) | 费用 (USD) |

### 公共字段

所有业务表继承 BaseDO，自动包含以下字段:

| 字段 | 类型 | 说明 |
|------|------|------|
| `create_time` | datetime | 创建时间 (自动填充) |
| `update_time` | datetime | 更新时间 (自动填充) |
| `creator` | varchar(64) | 创建者 ID |
| `updater` | varchar(64) | 更新者 ID |
| `deleted` | bit(1) | 逻辑删除 (0=正常, 1=已删除) |

部分表额外声明 `tenant_id` (多租户标识)。

---

## 前端

前端页面已全部创建（26 个 Vue 页面 + 8 个 API 服务文件），但尚未集成到完整的 yudao-ui-admin-vue3 项目中。

### 集成步骤

```bash
# 1. 克隆完整前端项目
git clone https://gitee.com/yudaocode/yudao-ui-admin-vue3.git
cd yudao-ui-admin-vue3

# 2. 复制 Amazon 模块文件
cp -r <本项目路径>/yudao-ui/yudao-ui-admin-vue3/src/api/amazon/ src/api/amazon/
cp -r <本项目路径>/yudao-ui/yudao-ui-admin-vue3/src/views/amazon/ src/views/amazon/

# 3. 安装依赖并启动
npm install
npm install echarts
npm run dev
```

详细路由配置和集成指南见: `yudao-ui/yudao-ui-admin-vue3/FRONTEND_INTEGRATION_GUIDE.md`

### 页面清单

| 模块 | 页面 | 功能 |
|------|------|------|
| Shop | index.vue, ShopForm.vue, ApiMonitor.vue | 店铺 CRUD, API 调用监控 |
| Listing | index.vue, ProductForm.vue, ProductDetail.vue, AutoPriceRule.vue | 商品管理, 版本历史, 自动定价 |
| Order | index.vue, OrderDetail.vue, FbaShipment.vue, OrderStats.vue | 订单列表, FBA 发货, 销售统计 |
| Inventory | index.vue, InventoryForecast.vue, ReplenishAlert.vue | 库存监控, 预测, 补货预警 |
| Research | index.vue, NicheDetail.vue, ProductOpportunity.vue | 市场分析, BSR, 选品推荐 |
| Ad | index.vue, AdReport.vue, AdRule.vue | 广告管理, 报表, 自动化规则 |
| Review | index.vue, ReviewAlert.vue, CustomerTemplate.vue | 评论管理, 预警, 消息模板 |
| Report | Dashboard.vue, WeeklyReport.vue, AiChat.vue | 运营看板, AI 周报, AI 助手 |

---

## 核心功能说明

### SP-API 客户端

`SpApiClient` (585 行) 实现了完整的 Amazon SP-API 调用能力:

- AWS Signature V4 请求签名
- 可配置令牌桶限流 (Redis + Lua 分布式)
- 指数退避 + 全抖动重试策略
- OAuth2 LWA 授权码交换 + Token 自动刷新
- AES-256-GCM 凭证加密存储
- HTTP 代理支持

### 数据同步框架

`AbstractAmazonSyncJob` 提供统一的同步任务基类:

- 自动遍历所有已启用店铺 (status=1)
- Per-shop 错误隔离 (单店铺失败不影响其他)
- JSON 参数解析 (支持 shopId/marketplaceId/lastSyncTime 过滤)
- 通过 Quartz 定时调度, Bean 名称注册

已实现的同步 Job:

| Bean 名称 | 模块 | 同步内容 |
|-----------|------|----------|
| `amazonOrderSyncJob` | order | 订单 + 行项目 |
| `amazonInventorySyncJob` | inventory | FBA 库存快照 |
| `amazonListingSyncJob` | listing | 商品 Listing |
| `amazonReviewSyncJob` | review | 客户评论 |
| `amazonAdReportSyncJob` | ad | 广告日度报表 |

在 Quartz 管理界面添加定时任务时，处理器填写对应的 Bean 名称即可。

### AI 能力层

`OpsAgent` 注册了 8 个 Function Calling 工具:

| 工具 | 功能 |
|------|------|
| AnalyzeReviews | 分析评论情感和关键问题 |
| ForecastInventory | 预测库存消耗和补货时间 |
| GenerateListing | AI 生成 Listing 文案 |
| GetAdPerformance | 获取广告投放效果 |
| GetInventoryStatus | 查询库存状态 |
| GetKeywordData | 获取关键词搜索数据 |
| GetSalesSummary | 获取销售汇总 |
| GetTopProducts | 获取热销商品排行 |

`PromptTemplateEngine` 支持 `{{variable}}` 变量替换和 `{{#if condition}}` 条件渲染。

---

## 已知问题

### 阻塞性问题

| 问题 | 说明 | 解决方案 |
|------|------|----------|
| Amazon 模块未接入 yudao-server | `yudao-server/pom.xml` 未声明 Amazon 依赖, 不会打包进 fat jar | 按"快速开始"第 4 步添加依赖 |
| Spring AI 依赖版本缺失 | amazon-ai 模块使用旧 artifact 名且未声明版本 | 在 yudao-dependencies/pom.xml 中添加 BOM 管理, 或改用 1.1.5 版本新命名 |
| SP-API 凭证为占位值 | 所有 Amazon/AWS 凭证为占位符 | 从 Amazon Seller Central 获取真实凭证 |

### TODO 统计 (43 个)

| 优先级 | 类别 | 数量 | 说明 |
|--------|------|------|------|
| P0 | SP-API 实际调用 | 20 | 订单/库存/Listing/评论/广告的数据同步 |
| P1 | AI 模型调用 | 10 | OpsAgent Function 实现、AI 服务接入真实模型 |
| P1 | Agent 工具接线 | 7 | 8 个 Function 工具中 7 个为桩实现 |
| P2 | 业务逻辑 | 6 | 规则引擎执行、自动定价、财务预测 |

### 代码卫生

| 问题 | 建议 |
|------|------|
| common-core 双包名 (com.yudao vs cn.iocoder.yudao) | 统一到 cn.iocoder.yudao |
| @Component 注解冗余 | 删除, AutoConfiguration 已管理 |
| common-encrypt/model 空壳 | 合并到 common-core 或填充实际内容 |
| amazon-spapi 全部空壳 | 移除或填充 SP-API 封装层 |
| Marketplace 种子数据不一致 (shop.sql 20 条 vs amazon.sql 19 条) | 统一 |

---

## 开发路线图

### Phase A: SP-API 实际集成 [P0, 预估 3-5 天]

1. 订单同步 — `OrderSyncServiceImpl.syncOrders()` → SP-API Orders API
2. 库存同步 — `InventorySyncServiceImpl.syncInventory()` → FBA Inventory API
3. Listing 同步 — `ListingSyncServiceImpl` → Catalog Items API
4. 评论同步 — `ReviewSyncServiceImpl` → Product Reviews API
5. 广告同步 — `AdReportSyncServiceImpl` → Advertising API (异步: 提交→轮询→下载)

### Phase B: AI 模型调用接入 [P1, 预估 2-3 天]

1. 修复 Spring AI 依赖版本
2. AiListingGeneratorService 接入 PromptTemplateEngine + AiModelRouter
3. AiReviewAnalysisService / AiAdOptimizerService / AiInventoryForecastService 接入
4. OpsAgent 8 个 Function 工具填充真实实现

### Phase C: 前端集成 [P1, 预估 1-2 天]

1. 克隆 yudao-ui-admin-vue3 上游项目
2. 复制 Amazon 模块文件 + 配置路由
3. 在后台菜单管理中配置 Amazon 菜单和权限
4. 验证页面渲染和 API 对接

### Phase D: 业务逻辑完善 [P2, 预估 3-4 天]

1. AdRuleEngine — 解析 condition/action JSON, 执行规则动作
2. AutoPriceService — 扫描启用规则, 评估条件, 执行调价
3. FinancialProjection — 动态计算启动成本/回本周/12 月利润
4. AiWeeklyReportService — 聚合各模块数据 + AI 生成摘要

### Phase E: 测试与质量 [P2]

1. 补充 SP-API 集成测试
2. 补充 AI Agent E2E 测试
3. 代码卫生清理 (双包名、空模块、冗余注解)
4. 统一 Marketplace 种子数据

---

## 相关文档

- [PROJECT_CONTEXT.md](./PROJECT_CONTEXT.md) — 完整项目上下文 (开发交接文档)
- [前端集成指南](./yudao-ui/yudao-ui-admin-vue3/FRONTEND_INTEGRATION_GUIDE.md) — Vue 前端集成步骤

## 许可

本项目基于 [ruoyi-vue-pro](https://gitee.com/zhijiantianya/ruoyi-vue-pro) 开源框架开发, 遵循其 [MIT License](https://gitee.com/zhijiantianya/ruoyi-vue-pro/blob/master/LICENSE) 开源协议。
