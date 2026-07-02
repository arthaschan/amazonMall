# AmazonOps AI 前端集成指南

## 概述

本项目 (`yudao-ui/yudao-ui-admin-vue3/`) 包含 AmazonOps AI 全部 8 个业务模块的前端页面，共 34 个文件（8 个 API 服务 + 26 个 Vue 页面）。

当前为独立开发状态，需要集成到完整的 yudao-ui-admin-vue3 项目中才能运行。

## 集成步骤

### 1. 克隆完整前端项目

```bash
# 从上游仓库克隆完整项目
git clone https://gitee.com/yudaocode/yudao-ui-admin-vue3.git

# 或者使用 GitHub
git clone https://github.com/yudaocode/yudao-ui-admin-vue3.git
```

### 2. 复制 Amazon 模块文件

```bash
# 将 Amazon 模块的 API 和 Views 复制到完整项目中
cp -r yudao-ui/yudao-ui-admin-vue3/src/api/amazon/ <完整项目路径>/src/api/amazon/
cp -r yudao-ui/yudao-ui-admin-vue3/src/views/amazon/ <完整项目路径>/src/views/amazon/
```

### 3. 配置路由

在完整项目的路由配置文件中（通常在 `src/router/modules/` 下），添加 Amazon 模块路由：

```typescript
// src/router/modules/amazon.ts
export default {
  path: '/amazon',
  component: Layout,
  name: 'Amazon',
  meta: {
    title: 'Amazon 运营',
    icon: 'ep:shopping-cart'
  },
  children: [
    // Shop 店铺管理
    {
      path: 'shop',
      name: 'AmazonShop',
      component: () => import('@/views/amazon/shop/index.vue'),
      meta: { title: '店铺管理', icon: 'ep:shop' }
    },
    {
      path: 'api-monitor',
      name: 'AmazonApiMonitor',
      component: () => import('@/views/amazon/shop/ApiMonitor.vue'),
      meta: { title: 'API 监控', icon: 'ep:monitor' }
    },
    // Listing 商品刊登
    {
      path: 'listing',
      name: 'AmazonListing',
      component: () => import('@/views/amazon/listing/index.vue'),
      meta: { title: '商品管理', icon: 'ep:goods' }
    },
    {
      path: 'auto-price',
      name: 'AmazonAutoPrice',
      component: () => import('@/views/amazon/listing/AutoPriceRule.vue'),
      meta: { title: '自动定价', icon: 'ep:price-tag' }
    },
    // Order 订单管理
    {
      path: 'order',
      name: 'AmazonOrder',
      component: () => import('@/views/amazon/order/index.vue'),
      meta: { title: '订单管理', icon: 'ep:document' }
    },
    {
      path: 'fba-shipment',
      name: 'AmazonFbaShipment',
      component: () => import('@/views/amazon/order/FbaShipment.vue'),
      meta: { title: 'FBA 发货', icon: 'ep:van' }
    },
    {
      path: 'order-stats',
      name: 'AmazonOrderStats',
      component: () => import('@/views/amazon/order/OrderStats.vue'),
      meta: { title: '订单统计', icon: 'ep:data-line' }
    },
    // Inventory 库存管理
    {
      path: 'inventory',
      name: 'AmazonInventory',
      component: () => import('@/views/amazon/inventory/index.vue'),
      meta: { title: '库存管理', icon: 'ep:box' }
    },
    {
      path: 'inventory-forecast',
      name: 'AmazonInventoryForecast',
      component: () => import('@/views/amazon/inventory/InventoryForecast.vue'),
      meta: { title: '库存预测', icon: 'ep:trend-charts' }
    },
    {
      path: 'replenish-alert',
      name: 'AmazonReplenishAlert',
      component: () => import('@/views/amazon/inventory/ReplenishAlert.vue'),
      meta: { title: '补货预警', icon: 'ep:bell' }
    },
    // Research 选品分析
    {
      path: 'research',
      name: 'AmazonResearch',
      component: () => import('@/views/amazon/research/index.vue'),
      meta: { title: '市场分析', icon: 'ep:search' }
    },
    {
      path: 'product-opportunity',
      name: 'AmazonProductOpportunity',
      component: () => import('@/views/amazon/research/ProductOpportunity.vue'),
      meta: { title: '选品推荐', icon: 'ep:star' }
    },
    // Ad 广告管理
    {
      path: 'ad',
      name: 'AmazonAd',
      component: () => import('@/views/amazon/ad/index.vue'),
      meta: { title: '广告管理', icon: 'ep:promotion' }
    },
    {
      path: 'ad-report',
      name: 'AmazonAdReport',
      component: () => import('@/views/amazon/ad/AdReport.vue'),
      meta: { title: '广告报表', icon: 'ep:pie-chart' }
    },
    {
      path: 'ad-rule',
      name: 'AmazonAdRule',
      component: () => import('@/views/amazon/ad/AdRule.vue'),
      meta: { title: '广告规则', icon: 'ep:set-up' }
    },
    // Review 评论管理
    {
      path: 'review',
      name: 'AmazonReview',
      component: () => import('@/views/amazon/review/index.vue'),
      meta: { title: '评论管理', icon: 'ep:chat-dot-round' }
    },
    {
      path: 'review-alert',
      name: 'AmazonReviewAlert',
      component: () => import('@/views/amazon/review/ReviewAlert.vue'),
      meta: { title: '评论预警', icon: 'ep:bell' }
    },
    {
      path: 'customer-template',
      name: 'AmazonCustomerTemplate',
      component: () => import('@/views/amazon/review/CustomerTemplate.vue'),
      meta: { title: '消息模板', icon: 'ep:document-copy' }
    },
    // Report 报表
    {
      path: 'dashboard',
      name: 'AmazonDashboard',
      component: () => import('@/views/amazon/report/Dashboard.vue'),
      meta: { title: '运营看板', icon: 'ep:data-board' }
    },
    {
      path: 'weekly-report',
      name: 'AmazonWeeklyReport',
      component: () => import('@/views/amazon/report/WeeklyReport.vue'),
      meta: { title: '周报', icon: 'ep:document' }
    },
    {
      path: 'ai-chat',
      name: 'AmazonAiChat',
      component: () => import('@/views/amazon/report/AiChat.vue'),
      meta: { title: 'AI 助手', icon: 'ep:cpu' }
    }
  ]
}
```

### 4. 安装依赖

确保项目已安装以下额外依赖（如未安装）：

```bash
npm install echarts  # 用于图表展示
```

### 5. 配置权限菜单

在后台管理系统的「菜单管理」中，添加 Amazon 相关菜单项，配置对应的权限标识（如 `amazon:shop:create`、`amazon:listing:update` 等）。

### 6. 启动项目

```bash
npm install
npm run dev
```

## 模块清单

| 模块 | API 文件 | Vue 页面 | 页面数 |
|------|----------|----------|--------|
| Shop 店铺管理 | shop/index.ts | index.vue, ShopForm.vue, ApiMonitor.vue | 3 |
| Listing 商品刊登 | listing/index.ts | index.vue, ProductForm.vue, ProductDetail.vue, AutoPriceRule.vue | 4 |
| Order 订单管理 | order/index.ts | index.vue, OrderDetail.vue, FbaShipment.vue, OrderStats.vue | 4 |
| Inventory 库存管理 | inventory/index.ts | index.vue, InventoryForecast.vue, ReplenishAlert.vue | 3 |
| Research 选品分析 | research/index.ts | index.vue, NicheDetail.vue, ProductOpportunity.vue | 3 |
| Ad 广告管理 | ad/index.ts | index.vue, AdReport.vue, AdRule.vue | 3 |
| Review 评论管理 | review/index.ts | index.vue, ReviewAlert.vue, CustomerTemplate.vue | 3 |
| Report 报表 | report/index.ts | Dashboard.vue, WeeklyReport.vue, AiChat.vue | 3 |
| **合计** | **8** | **26** | **26** |

## 技术栈

- Vue 3 (Composition API + `<script setup>`)
- TypeScript
- Element Plus (UI 组件库)
- ECharts (图表)
- Axios (HTTP 请求，通过 `@/config/axios`)

## 依赖说明

页面组件依赖 yudao-ui-admin-vue3 框架提供的：
- `ContentWrap` — 内容容器组件
- `Pagination` — 分页组件
- `useMessage()` — 消息提示 Hook
- `useI18n()` — 国际化 Hook
- `dateFormatter` — 日期格式化工具
- `v-hasPermi` — 权限指令
- `Dialog` — 弹窗组件

## 已知限制

1. 图表使用 ECharts 直接渲染，未封装为独立组件
2. 部分表单验证规则需要根据实际业务调整
3. AI Chat 页面的 Markdown 渲染依赖 `marked` 库（需确认是否已安装）
4. 路由配置需要根据实际项目结构调整路径和组件引用
