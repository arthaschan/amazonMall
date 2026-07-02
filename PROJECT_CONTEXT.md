# AmazonOps AI — 完整项目上下文

> **最后更新: 2026-07-02 | Git Commit: 3024477 | 仓库: /Users/arthas/git/amazonMall/**

---

## 1. 项目概述

AmazonOps AI 是基于 **ruoyi-vue-pro (Yudao)** 框架构建的 Amazon 卖家全链路运营平台。涵盖选品调研、Listing优化、订单管理、库存预测、广告自动化、评论分析、AI决策支持。

| 属性 | 值 |
|------|---|
| 基础框架 | ruoyi-vue-pro 2026.06-jdk8-SNAPSHOT |
| 后端 | Spring Boot 2.7.18 + MyBatis Plus + JDK 8 |
| 前端 | Vue 3.2 + Element Plus (**未启动**) |
| 数据库 | MySQL 8.x + Redis |
| 定时任务 | Quartz (yudao-spring-boot-starter-job) |
| AI框架 | Spring AI (ChatClient + Function Calling) |
| SP-API | OkHttp + AWS Signature V4 签名 |

---

## 2. 模块架构完整清单

### 2.1 基础设施模块

#### amazon-common (4子模块, 18个Java文件)

**common-core (10 main / 3 test)** — SP-API核心, 全部生产级实现:

- `SpApiClient` (585行) — AWS SigV4签名 + 限流 + 重试 + 代理, GET/POST/PUT/DELETE/PATCH
- `SpApiException` (169行) — 密封异常层次: Auth/RateLimit/Server/Client
- `SpApiRateLimiter` (278行) — Redis+Lua分布式令牌桶, 响应头动态调整
- `SpApiRetryPolicy` (189行) — 指数退避+全抖动, 可重试/不可重试状态码集
- `SpApiTokenRefresher` (242行) — LWA OAuth2授权码交换+刷新+重试
- `SpApiTokenStore` (318行) — Redis缓存AccessToken + MySQL持久化加密RefreshToken
- `AmazonCredentialEncryptor` (183行) — AES-256-GCM加密, IV前置, Base64编码
- `AmazonProperties` (256行) — 完整配置绑定(凭证/AWS/OAuth/代理/限流/重试/区域端点)
- 包名: 实现在 `com.yudao.module.amazon.common.core`, AutoConfiguration在 `cn.iocoder.yudao.module.amazon.common.core` (双包, 可编译但不优雅)

**common-sync (4 main / 1 test)** — 同步框架:

- `AbstractAmazonSyncJob` (120行) — 实现JobHandler, 自动遍历所有已启用店铺, per-shop错误隔离, JSON参数解析
- `SyncJobParam` — DTO(shopId, marketplaceId, lastSyncTime)
- SyncFrameworkTest (14个测试方法)

**common-encrypt / common-model** — 空壳, 无实际代码

#### amazon-spapi (3子模块, 6个Java文件) — 全部空壳

spapi-auth/client/ads 均只有空AutoConfiguration。所有SP-API逻辑在common-core。

### 2.2 业务模块 (8个)

| 模块 | Main | Test | Controller | Service对 | DO | SQL表 | 完成度 | 实现深度 |
|------|------|------|-----------|----------|-----|-------|--------|---------|
| shop | 23 | 1 | 2 | 2+2 | 3 | 3 | **100%** | CRUD+OAuth全链路+Token管理+凭证加密 |
| research | 36 | 3 | 2 | 6+6 | 5 | 5 | **95%** | 全知评分算法+BSR回归+财务预测, SP-API集成TODO |
| listing | 33 | 1 | 3 | 7+7 | 3 | 3 | **90%** | CRUD+版本控制, AI生成/SP-API同步TODO |
| order | 30 | 1 | 3 | 4+4 | 3 | 3 | **85%** | CRUD完整, OrderSyncService为TODO桩 |
| inventory | 31 | 1 | 3 | 5+5 | 3 | 3 | **85%** | CRUD+滞销检测, InventorySync为TODO桩 |
| ad | 37 | 1 | 4 | 6+6 | 4 | 4 | **85%** | CRUD+规则引擎CRUD, SP-API同步/规则执行TODO |
| review | 30 | 1 | 4 | 5+5 | 3 | 3 | **85%** | CRUD+模板管理, AI分析/SP-API同步TODO |
| report | 24 | 3 | 3 | 3+3 | 2 | 2 | **80%** | Dashboard聚合查询已实现, AiChat/AiWeekly为占位符 |

### 2.3 AI模块 (amazon-ai, 7子模块, 39 main / 4 test)

| 子模块 | 关键类 | 状态 |
|--------|-------|------|
| ai-core (10/2) | AiModelRouter(194行), PromptTemplateEngine(203行), AiProperties(121行), AiTokenTracker, AiResponseParser, AiTaskType(9种) | ✅ 生产级 |
| ai-agent (11/1) | OpsAgent(305行) + 8个Function工具(GetSalesSummary/GetTopProducts/GetInventoryStatus/GetAdPerformance/AnalyzeReviews/GenerateListing/GetKeywordData/ForecastInventory) | ✅ Agent生产级, Function工具为桩 |
| ai-listing (5/1) | AiListingGeneratorService, ListingDiagnosisService | 桩 |
| ai-research (5/0) | AiProductBlueprintService, AiRecommendationEngineService | 桩 |
| ai-ad (4/0) | AiAdOptimizerService | 桩 |
| ai-inventory (4/0) | AiInventoryForecastService | 桩 |
| ai-review (4/0) | AiReviewAnalysisService | 桩 |

**Prompt模板(7个.st):** ad_optimization, chat_assistant, listing_diagnosis, listing_generator, product_blueprint, review_analysis, weekly_report

### 2.4 Sync Job实现 (5个)

全部继承AbstractAmazonSyncJob, 实现doSync(AmazonShopDO)+getJobName():

| Job类 | Bean名 | 所在模块 | doSync状态 |
|-------|--------|---------|-----------|
| AmazonOrderSyncJob | amazonOrderSyncJob | order | 调用OrderSyncService(桩) |
| AmazonInventorySyncJob | amazonInventorySyncJob | inventory | TODO桩 |
| AmazonListingSyncJob | amazonListingSyncJob | listing | TODO桩 |
| AmazonReviewSyncJob | amazonReviewSyncJob | review | TODO桩 |
| AmazonAdReportSyncJob | amazonAdReportSyncJob | ad | TODO桩 |

### 2.5 数据库 (9个SQL文件, 27张表, 727行DDL)

所有DDL遵循约定: CREATE TABLE IF NOT EXISTS, utf8mb4_unicode_ci, BaseDO五字段(create_time, update_time, creator, updater, deleted), tenant_id在需要的表上, 合理索引, COMMENT。

合并文件: `sql/mysql/amazon.sql`

### 2.6 测试 (19个文件, 249个方法)

覆盖模块: common-core(3), common-sync(1), shop(1), research(3), listing(1), order(1), inventory(1), ad(1), review(1), report(3), ai-core(2), ai-agent(1), ai-listing(1)

零测试: amazon-spapi, common-encrypt, common-model, ai-research/ad/inventory/review子模块

---

## 3. 配置与启动

### 3.1 启动前必须完成的配置

文件: `yudao-server/src/main/resources/application-local.yaml` (已添加模板)

```yaml
amazon:
  sp-api:
    app-id: amzn1.application.xxx        # ← Amazon Seller Central
    client-id: amzn1.application-oa2-client.xxx  # ← LWA凭证
    client-secret: YOUR_CLIENT_SECRET_HERE
    aws-access-key-id: YOUR_AWS_ACCESS_KEY_ID    # ← AWS IAM
    aws-secret-access-key: YOUR_AWS_SECRET_ACCESS_KEY
    aws-role-arn: arn:aws:iam::YOUR_ROLE_ARN
    redirect-uri: http://localhost:48080/admin-api/amazon/shop/callback
    default-marketplace-id: ATVPDKIKX0DER
    sandbox: true
    encryption-key: BASE64_ENCODED_32BYTE_KEY  # AES-256
    max-retries: 3
  ai:
    openai:
      api-key: YOUR_OPENAI_API_KEY_HERE
      default-model: gpt-4o
    budget:
      monthly-limit-usd: 100.0
  sync:
    enabled: true
    default-delta-window: 60m
```

### 3.2 数据库初始化

```bash
mysql -u root -p ruoyi-vue-pro-jdk8 < sql/mysql/ruoyi-vue-pro.sql  # 基座表
mysql -u root -p ruoyi-vue-pro-jdk8 < sql/mysql/amazon.sql         # Amazon 27张表
```

### 3.3 启动命令

```bash
mvn clean install -DskipTests
# 然后启动 yudao-server (Spring Boot Application)
# 默认端口: 48080
```

---

## 4. 已知问题

### 4.1 已修复

- ~~common-sync pom.xml缺少shop模块依赖~~ → 已添加

### 4.2 代码卫生问题 (非阻塞)

| 问题 | 位置 | 建议 |
|------|------|------|
| common-core双包名 | AutoConfiguration在cn.iocoder, 实现在com.yudao | 统一到cn.iocoder |
| @Component注解冗余 | SpApiClient等类 | 删除@Component, AutoConfiguration已管理Bean |
| common-encrypt/model空壳 | 2个子模块 | 合并到common-core或填充实际内容 |
| amazon-spapi全部空壳 | 3个子模块 | 移除或填充SP-API封装层 |
| spring.factories双重注册 | META-INF | 保留.imports, 删除spring.factories |
| Listing模块重复SyncJob | AmazonListingSyncJob + ListingSyncJob | 合并为一个 |

---

## 5. 下一步开发路线图 (优先级排序)

### Phase A: SP-API实际集成 [P0, 预估3-5天]

这是当前最大的阻塞项。43个TODO中约20个与SP-API调用相关。

**推荐实现顺序:**

1. **订单同步** — `OrderSyncServiceImpl.syncOrders()`
   - SP-API: `GET /orders/v0/orders?MarketplaceIds={id}&CreatedAfter={date}`
   - 分页: NextToken模式
   - 映射: AmazonOrderDO + AmazonOrderItemDO
   - 完成后: AmazonOrderSyncJob自动生效

2. **库存同步** — `InventorySyncServiceImpl.syncInventory()`
   - SP-API: `GET /fba/inventory/v1/summaries`
   - 映射: AmazonInventoryDO

3. **Listing同步** — `ListingSyncServiceImpl`
   - SP-API: `GET /catalog/2022-04-01/items`
   - 映射: AmazonProductDO

4. **评论同步** — `ReviewSyncServiceImpl`
   - SP-API: Product Reviews API (需确认具体端点)

5. **广告同步** — `AdReportSyncServiceImpl`
   - Advertising API: `POST /v2/sp/report` (异步: 提交→轮询→下载)

**SpApiClient已提供的方法:** `get(sellerId, region, path, queryParams)`, `post(sellerId, region, path, body)`, `put(...)`, `delete(...)` — 直接调用即可。

### Phase B: AI模型调用接入 [P1, 预估2-3天]

1. **AI Agent Function工具连接** — 7个Function需要注入实际Service
   - GetSalesSummaryFunction → 注入OrderService, 聚合查询
   - GetTopProductsFunction → 注入ProductListingService + OrderService
   - GetInventoryStatusFunction → 注入InventoryService
   - GetAdPerformanceFunction → 注入AdCampaignService + AdReportService
   - AnalyzeReviewsFunction → 注入ReviewService
   - ForecastInventoryFunction → 注入InventoryForecastService
   - GetKeywordDataFunction → 注入KeywordResearchService

2. **AiChatService** — 调用OpsAgent.chat()替代硬编码占位符

3. **AiWeeklyReportService** — 从各模块聚合数据 + 调用AI生成摘要

4. **领域AI服务** — AiListingGenerator, AiReviewAnalysis, AiAdOptimizer 接入PromptTemplateEngine + AiModelRouter

### Phase C: 前端Vue页面 [P0, 预估5-7天]

**完全未启动**, 是当前最大阻塞项之一。

1. 初始化Vue3项目: `yudao-ui/yudao-ui-admin-vue3/` (npm init, 配置路由/菜单)
2. 页面结构 (按菜单):
   - 店铺管理: 店铺列表(OAuth绑定), API监控
   - 选品调研: 品类分析, 机会评估, 评分面板
   - 商品管理: Listing列表, 版本历史, 自动调价
   - 订单管理: 订单列表, FBA货件, 订单统计
   - 库存管理: 库存概览, 预测, 补货预警
   - 广告管理: 活动列表, 报告, 规则, 搜索词
   - 评论管理: 评论列表, AI分析, 预警, 模板
   - 数据报表: 仪表盘, AI对话, AI周报
3. API对接: 创建 `src/api/amazon/` 下各模块API文件

### Phase D: 业务逻辑完善 [P2, 预估3-4天]

1. AdRuleEngine — 解析conditionJson/actionJson, 执行规则动作(调价/暂停/加预算)
2. AutoPriceService — 扫描启用规则, 评估条件, 执行调价
3. FinancialProjection — 动态计算启动成本/回本周/12月利润/品牌估值
4. InventoryForecast — 基于历史销量时间序列预测(移动平均/指数平滑)
5. SlowMovingDetector — 查询历史销量, 标记滞销品
6. ReplenishAlert — 扫描库存vs再订购点, 自动生成预警
7. ProfitCalculator — 根据尺寸/重量计算FBA费用

### Phase E: 测试与质量 [P2]

1. 补充amazon-spapi模块测试
2. OpsAgent集成测试 (端到端Function Calling)
3. 各SyncJob集成测试 (Mock SpApiClient)
4. 代码卫生: 统一包名, 清理冗余@Component, 移除空壳模块

---

## 6. 关键技术细节速查

### 6.1 SpApiClient使用示例

```java
@Resource
private SpApiClient spApiClient;

// GET 请求
JsonNode orders = spApiClient.get(
    shop.getSellerId(),                    // Amazon Seller ID
    "us-east-1",                           // AWS Region
    "/orders/v0/orders",                   // API Path
    Map.of("MarketplaceIds", "ATVPDKIKX0DER", "CreatedAfter", "2026-01-01")
);

// POST 请求
JsonNode result = spApiClient.post(
    shop.getSellerId(), "us-east-1",
    "/reports/2021-06-30/reports",
    Map.of("reportType", "GET_MERCHANT_LISTINGS_ALL_DATA", "marketplaceIds", List.of("ATVPDKIKX0DER"))
);
```

### 6.2 SyncJob注册方式

在Yudao管理后台 → 定时任务 → 新增:
- 处理器名称: `amazonOrderSyncJob` (Spring Bean名)
- Cron: `0 0 */2 * * ?` (每2小时)
- 参数: `{"shopId": 1}` 或留空(自动遍历所有店铺)
- 重试次数: 3
- 重试间隔: 5000ms

### 6.3 AI Agent使用

```java
@Resource
private OpsAgent opsAgent;

// 同步调用
String response = opsAgent.chat(sessionId, "分析一下最近的销售趋势", tenantId);

// 流式调用
Flux<String> stream = opsAgent.chatStream(sessionId, "推荐优化方案", tenantId);
```

### 6.4 PromptTemplate使用

```java
@Resource
private PromptTemplateEngine templateEngine;

// 从classpath加载模板(prompts/listing_generator.st)
String result = templateEngine.render("listing_generator", Map.of(
    "product_name", "Widget Pro",
    "keywords", "premium, durable, eco-friendly"
));

// 从字符串渲染
String result = templateEngine.renderString("Hello {{name}}!", Map.of("name", "Seller"));
```

### 6.5 核心包名速查

| 模块 | 包名前缀 |
|------|---------|
| 业务模块 | cn.iocoder.yudao.module.amazon.{module} |
| AI | cn.iocoder.yudao.module.amazon.ai.{submodule} |
| common-core实现 | com.yudao.module.amazon.common.core (注意: com不是cn) |
| common-sync/encrypt/model | cn.iocoder.yudao.module.amazon.common.{submodule} |

---

## 7. 统计总览

| 指标 | 数值 |
|------|------|
| Amazon模块总数 | 11 (2基础 + 8业务 + 1AI) |
| Java文件(main) | 337 |
| Java文件(test) | 19 |
| Controller | 24 |
| Service对 | 38+ |
| DO数据对象 | 27 |
| Mapper | 27 |
| SQL表 | 27 |
| SQL DDL行数 | 727 |
| Sync Job实现 | 5 |
| AI Function工具 | 8 |
| Prompt模板 | 7 |
| 测试方法总数 | 249 |
| TODO总数 | 43 |
| Git Commits | 1 (3024477) |
| Vue前端页面 | 0 |

**后端架构完成度: ~95% (缺SP-API实际调用)**
**AI框架完成度: ~70% (核心已建, Function工具为桩)**
**前端完成度: 0%**
**整体项目完成度: ~60%**

---

*此文档由 AmazonOps AI Progress Tracker 于 2026-07-02 生成, 用于项目交接和未来研发续接。*
