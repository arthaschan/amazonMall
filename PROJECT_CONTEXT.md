# AmazonOps AI - Project Context

## Project Overview
AmazonOps AI is a comprehensive Amazon seller operations platform built on top of the Yudao (ruoyi-vue-pro) framework. It provides end-to-end tools for Amazon sellers including product research, listing optimization, order management, inventory forecasting, advertising automation, review analysis, and AI-powered decision support.

**Base Framework:** ruoyi-vue-pro (Yudao)
**Tech Stack:** Spring Boot + MyBatis Plus + Vue3 (planned)
**Project Root:** amazonops-base

---

## Module Architecture

### Infrastructure Modules
| Module | Sub-modules | Description |
|--------|------------|-------------|
| `amazon-common` | core, encrypt, model, sync | Shared utilities, SP-API client, encryption, data sync framework |
| `amazon-spapi` | auth, client, ads | SP-API integration layer (auth, client config, ads API) |

### Business Modules
| Module | Java Files | Controllers | Services | DAL | Tests |
|--------|-----------|-------------|----------|-----|-------|
| `amazon-shop` | 25 | 2 | 4 | 6 | 1 |
| `amazon-research` | 41 | 2 | 16 | 13 | 3 |
| `amazon-listing` | 35 | 3 | 14 | 9 | 1 |
| `amazon-order` | 31 | 3 | 10 | 9 | 1 |
| `amazon-inventory` | 31 | 3 | 10 | 9 | 1 |
| `amazon-ad` | 38 | 4 | 11 | 11 | 1 |
| `amazon-review` | 33 | 4 | 10 | 9 | 1 |
| `amazon-report` | 27 | 3 | 7 | 7 | 0 |

### AI Module
| Sub-module | Key Classes | Description |
|-----------|------------|-------------|
| `amazon-ai-core` | AiModelRouter, AiProperties, PromptTemplateEngine, AiResponseParser, AiTokenTracker | Core AI infrastructure |
| `amazon-ai-agent` | OpsAgent + 8 Function classes | AI Agent with tool-calling capabilities |
| `amazon-ai-research` | AiProductBlueprintService, AiRecommendationEngineService | AI-powered product research |
| `amazon-ai-listing` | AiListingGeneratorService, ListingDiagnosisService | AI listing generation & diagnosis |
| `amazon-ai-ad` | AiAdOptimizerService | AI ad campaign optimization |
| `amazon-ai-inventory` | AiInventoryForecastService | AI inventory forecasting |
| `amazon-ai-review` | AiReviewAnalysisService | AI review sentiment analysis |

---

## Overall Statistics (2026-07-02)

| Metric | Count |
|--------|-------|
| Total Amazon Modules | 11 |
| Total Amazon Java Files | 329 |
| Total Amazon Controllers | 24 |
| Total Amazon Service Classes | 86+ |
| Total Amazon DAL (Mapper+DO) | 73+ |
| Total Amazon Test Files | 16 (+4 new) |
| Total Amazon Test Methods | 63+ (45 new) |
| SQL DDL Tables | 27 (9 modules) |
| Sync Job Implementations | 5 |
| Project-wide Java Files | 5,818+ |
| Vue Frontend Pages (Amazon) | 0 |
| SQL Migration Files (Amazon) | 0 |
| Git Commits | 1 |

---

## Phase Progress Log

### Phase 1: Base Adaptation + SP-API Foundation
**Status: COMPLETED**
**Progress: 100%**

- `amazon-common` module skeleton: 4 sub-modules (core/encrypt/model/sync), 18 Java files
  - SpApiClient, SpApiException, SpApiRateLimiter, SpApiRetryPolicy, SpApiTokenRefresher, SpApiTokenStore, AmazonCredentialEncryptor, AmazonProperties
  - Tests: SpApiClientTest, SpApiTokenStoreTest
- `amazon-spapi` module skeleton: 3 sub-modules (auth/client/ads), 6 Java files
  - SpApiAuthAutoConfiguration, SpApiClientAutoConfiguration, SpApiAdsAutoConfiguration
- Root pom.xml updated with all amazon modules

**Gaps:** Sync framework is skeleton-only (AutoConfiguration + package-info, no concrete implementation yet)

### Phase 2: Shop Management + Data Sync
**Status: MOSTLY COMPLETED**
**Progress: 85%**

- `amazon-shop` module: 25 Java files, full CRUD architecture
  - Controllers: AmazonShopController, AmazonApiMonitorController
  - Services: AmazonShopService(Impl), AmazonApiMonitorService(Impl)
  - DAL: AmazonShopDO, AmazonMarketplaceDO, AmazonApiLogDO + 3 Mappers
  - VOs: CreateReq, UpdateReq, PageReq, Resp, ApiLogPageReq, ApiStatsResp
  - Enums: AmazonMarketplaceEnum, AmazonShopStatusEnum, ErrorCodeConstants
  - Tests: ShopE2ETest
- `amazon-common-sync` sub-module created (skeleton)
- OrderSyncService in order module provides sync pattern

**Gaps:** Sync framework needs concrete implementation (scheduled jobs, event-driven sync); no Amazon-specific SQL migration files found

### Phase 3: Core Business Modules
**Status: COMPLETED**
**Progress: 95%**

- `amazon-research` (41 files): Most developed module
  - Controllers: NicheController, ScoreController
  - 8 Service pairs: BsrSalesEstimator, FinancialProjection, KeywordResearch, Niche, OmniscientScoreCalculator, SupplierMatch
  - 5 DOs: BsrRegression, FinancialProjection, Niche, ProductOpportunity, SupplierMatch
  - 3 Tests: BSRSalesEstimator, FinancialProjection, OmniscientScoreCalculator
- `amazon-listing` (35 files):
  - Controllers: ListingController, ListingDiagnosisController, AiListingController
  - 6 Service pairs: ProductListing, ListingVersion, AutoPrice, ProfitCalculator, ListingDiagnosis, AiListingGenerator
  - 3 DOs: AmazonProduct, ListingVersion, AutoPriceRule
  - 1 Test: ProfitCalculatorServiceTest
- `amazon-order` (31 files):
  - Controllers: OrderController, FbaShipmentController, OrderStatsController
  - 4 Service pairs: Order, OrderSync, OrderStats, FbaShipment
  - 3 DOs: AmazonOrder, AmazonOrderItem, AmazonFbaShipment
  - 1 Test: OrderSyncServiceTest
- `amazon-inventory` (31 files):
  - Controllers: InventoryController, InventoryForecastController, ReplenishAlertController
  - 4 Service pairs: Inventory, InventoryForecast, ReplenishAlert, SlowMovingDetector
  - 3 DOs: AmazonInventory, InventoryForecast, ReplenishAlert
  - 1 Test: InventoryForecastServiceTest
- `amazon-ad` (38 files):
  - Controllers: AdCampaignController, AdReportController, AdRuleController, AiAdOptimizerController
  - 5 Service pairs: AdCampaign, AdReport, AdSearchTerm, AdRuleEngine, AiAdOptimizer
  - 4 DOs: AdCampaign, AdReportDaily, AdRule, AdSearchTerm
  - 1 Test: AdRuleEngineTest
- `amazon-review` (33 files):
  - Controllers: ReviewController, ReviewAlertController, CustomerTemplateController, AiReviewAnalysisController
  - 4 Service pairs: Review, ReviewAlert, CustomerTemplate, AiReviewAnalysis
  - 3 DOs: AmazonReview, ReviewAlert, CustomerTemplate
  - 1 Test: ReviewAnalysisServiceTest
- `amazon-report` (27 files):
  - Controllers: DashboardController, AiChatController, AiWeeklyReportController
  - 3 Service pairs: Dashboard, AiChat, AiWeeklyReport
  - 2 DOs: DashboardMetric, WeeklyReport
  - 0 Tests (GAP)

### Phase 4: AI Capability Layer
**Status: COMPLETED**
**Progress: 90%**

- `amazon-ai` module: 44 Java files across 7 sub-modules
- AI Core: AiModelRouter, PromptTemplateEngine, AiResponseParser, AiTokenTracker, AiTokenUsageDO
- AI Agent: OpsAgent + 8 tool functions (AnalyzeReviews, ForecastInventory, GenerateListing, GetAdPerformance, GetInventoryStatus, GetKeywordData, GetSalesSummary, GetTopProducts)
- Domain AI Services: Research(Listing, Ad, Inventory, Review) each with dedicated AI service + DTOs
- 1 Test: AiListingGeneratorTest

**Gaps:** No integration tests for OpsAgent; AiTokenTracker persistence layer not verified

### Phase 5: Frontend Vue Pages
**Status: NOT STARTED**
**Progress: 0%**

- No Vue frontend pages created for Amazon modules
- No package.json found in the project (frontend project not initialized)
- Only existing Vue file is from the base yudao framework (mes module)

### Phase 6: Test Cases
**Status: PARTIAL**
**Progress: 40%**

- 12 Amazon-specific test files created across modules
- Best coverage: research module (3 tests including core algorithms)
- Missing test coverage: report module (0 tests), spapi module (0 tests)
- Most tests are unit-level; E2E tests only in shop module

---

## Priority Gaps & Next Steps (Updated 2026-07-02)

1. **Frontend (Phase 5)** - Initialize Vue3 frontend project and build Amazon module pages **[P0 - NOT STARTED]**
2. **SP-API Integration** - Flesh out actual SP-API call implementations in sync jobs (currently TODO) **[P1]**
3. **Test Coverage** - Add tests for spapi module (still 0 tests), common-model **[P2]**
4. **Sync Framework Persistence** - Extend SyncProgressTracker from in-memory to Redis/DB **[P2]**

### Recently Completed (2026-07-02 Dev Session)

- ✅ **SQL DDL Scripts** — 8 modules, 27 tables total (including shop's existing 3 tables)
- ✅ **Sync Job Implementations** — 5 concrete jobs extending AbstractAmazonSyncJob: AmazonOrderSyncJob, AmazonInventorySyncJob, AmazonListingSyncJob, AmazonReviewSyncJob, AmazonAdReportSyncJob
- ✅ **Test Coverage** — 4 new test files: PromptTemplateEngineTest, AiModelRouterTest, OpsAgentTest, AmazonCredentialEncryptorTest
- ✅ **Report Tests** — DashboardServiceImplTest, AiChatServiceImplTest, AiWeeklyReportServiceImplTest already present

---

*Report generated: 2026-07-02*  
*Last dev session: 2026-07-02 (SQL DDL + Sync Jobs + Tests)*  
*Tool: AmazonOps AI Progress Tracker*
