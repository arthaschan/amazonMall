import Layout from '@/layout/Layout.vue'

/**
 * AmazonOps AI 路由配置
 *
 * 包含 8 大业务模块共 21 个子路由：
 *   店铺管理 / API 监控 / 商品管理 / 自动定价 / 订单管理 / FBA 发货 /
 *   订单统计 / 库存管理 / 库存预测 / 补货预警 / 市场分析 / 选品推荐 /
 *   广告管理 / 广告报表 / 广告规则 / 评论管理 / 评论预警 / 消息模板 /
 *   运营看板 / 周报 / AI 助手
 */
const amazonRoute = {
  path: '/amazon',
  component: Layout,
  redirect: '/amazon/dashboard',
  name: 'Amazon',
  meta: {
    title: 'Amazon 运营',
    icon: 'ep:shopping-cart'
  },
  children: [
    // ==================== Shop 店铺管理 ====================
    {
      path: 'shop',
      name: 'AmazonShop',
      component: () => import('@/views/amazon/shop/index.vue'),
      meta: { title: '店铺管理', icon: 'ep:shop', noCache: false }
    },
    {
      path: 'shop/form',
      name: 'AmazonShopForm',
      component: () => import('@/views/amazon/shop/ShopForm.vue'),
      meta: { title: '店铺表单', icon: 'ep:edit', noCache: true, hidden: true }
    },
    {
      path: 'api-monitor',
      name: 'AmazonApiMonitor',
      component: () => import('@/views/amazon/shop/ApiMonitor.vue'),
      meta: { title: 'API 监控', icon: 'ep:monitor', noCache: false }
    },

    // ==================== Listing 商品刊登 ====================
    {
      path: 'listing',
      name: 'AmazonListing',
      component: () => import('@/views/amazon/listing/index.vue'),
      meta: { title: '商品管理', icon: 'ep:goods', noCache: false }
    },
    {
      path: 'listing/form',
      name: 'AmazonListingForm',
      component: () => import('@/views/amazon/listing/ProductForm.vue'),
      meta: { title: '商品表单', icon: 'ep:edit', noCache: true, hidden: true }
    },
    {
      path: 'listing/detail/:id',
      name: 'AmazonListingDetail',
      component: () => import('@/views/amazon/listing/ProductDetail.vue'),
      meta: { title: '商品详情', icon: 'ep:view', noCache: true, hidden: true }
    },
    {
      path: 'auto-price',
      name: 'AmazonAutoPrice',
      component: () => import('@/views/amazon/listing/AutoPriceRule.vue'),
      meta: { title: '自动定价', icon: 'ep:price-tag', noCache: false }
    },

    // ==================== Order 订单管理 ====================
    {
      path: 'order',
      name: 'AmazonOrder',
      component: () => import('@/views/amazon/order/index.vue'),
      meta: { title: '订单管理', icon: 'ep:document', noCache: false }
    },
    {
      path: 'order/detail/:id',
      name: 'AmazonOrderDetail',
      component: () => import('@/views/amazon/order/OrderDetail.vue'),
      meta: { title: '订单详情', icon: 'ep:view', noCache: true, hidden: true }
    },
    {
      path: 'fba-shipment',
      name: 'AmazonFbaShipment',
      component: () => import('@/views/amazon/order/FbaShipment.vue'),
      meta: { title: 'FBA 发货', icon: 'ep:van', noCache: false }
    },
    {
      path: 'order-stats',
      name: 'AmazonOrderStats',
      component: () => import('@/views/amazon/order/OrderStats.vue'),
      meta: { title: '订单统计', icon: 'ep:data-line', noCache: false }
    },

    // ==================== Inventory 库存管理 ====================
    {
      path: 'inventory',
      name: 'AmazonInventory',
      component: () => import('@/views/amazon/inventory/index.vue'),
      meta: { title: '库存管理', icon: 'ep:box', noCache: false }
    },
    {
      path: 'inventory-forecast',
      name: 'AmazonInventoryForecast',
      component: () => import('@/views/amazon/inventory/InventoryForecast.vue'),
      meta: { title: '库存预测', icon: 'ep:trend-charts', noCache: false }
    },
    {
      path: 'replenish-alert',
      name: 'AmazonReplenishAlert',
      component: () => import('@/views/amazon/inventory/ReplenishAlert.vue'),
      meta: { title: '补货预警', icon: 'ep:bell', noCache: false }
    },

    // ==================== Research 选品分析 ====================
    {
      path: 'research',
      name: 'AmazonResearch',
      component: () => import('@/views/amazon/research/index.vue'),
      meta: { title: '市场分析', icon: 'ep:search', noCache: false }
    },
    {
      path: 'research/niche/:id',
      name: 'AmazonNicheDetail',
      component: () => import('@/views/amazon/research/NicheDetail.vue'),
      meta: { title: '细分市场详情', icon: 'ep:view', noCache: true, hidden: true }
    },
    {
      path: 'product-opportunity',
      name: 'AmazonProductOpportunity',
      component: () => import('@/views/amazon/research/ProductOpportunity.vue'),
      meta: { title: '选品推荐', icon: 'ep:star', noCache: false }
    },

    // ==================== Ad 广告管理 ====================
    {
      path: 'ad',
      name: 'AmazonAd',
      component: () => import('@/views/amazon/ad/index.vue'),
      meta: { title: '广告管理', icon: 'ep:promotion', noCache: false }
    },
    {
      path: 'ad-report',
      name: 'AmazonAdReport',
      component: () => import('@/views/amazon/ad/AdReport.vue'),
      meta: { title: '广告报表', icon: 'ep:pie-chart', noCache: false }
    },
    {
      path: 'ad-rule',
      name: 'AmazonAdRule',
      component: () => import('@/views/amazon/ad/AdRule.vue'),
      meta: { title: '广告规则', icon: 'ep:set-up', noCache: false }
    },

    // ==================== Review 评论管理 ====================
    {
      path: 'review',
      name: 'AmazonReview',
      component: () => import('@/views/amazon/review/index.vue'),
      meta: { title: '评论管理', icon: 'ep:chat-dot-round', noCache: false }
    },
    {
      path: 'review-alert',
      name: 'AmazonReviewAlert',
      component: () => import('@/views/amazon/review/ReviewAlert.vue'),
      meta: { title: '评论预警', icon: 'ep:bell', noCache: false }
    },
    {
      path: 'customer-template',
      name: 'AmazonCustomerTemplate',
      component: () => import('@/views/amazon/review/CustomerTemplate.vue'),
      meta: { title: '消息模板', icon: 'ep:document-copy', noCache: false }
    },

    // ==================== Report 报表 ====================
    {
      path: 'dashboard',
      name: 'AmazonDashboard',
      component: () => import('@/views/amazon/report/Dashboard.vue'),
      meta: { title: '运营看板', icon: 'ep:data-board', noCache: false }
    },
    {
      path: 'weekly-report',
      name: 'AmazonWeeklyReport',
      component: () => import('@/views/amazon/report/WeeklyReport.vue'),
      meta: { title: '周报', icon: 'ep:document', noCache: false }
    },
    {
      path: 'ai-chat',
      name: 'AmazonAiChat',
      component: () => import('@/views/amazon/report/AiChat.vue'),
      meta: { title: 'AI 助手', icon: 'ep:cpu', noCache: true }
    }
  ]
}

export default amazonRoute
