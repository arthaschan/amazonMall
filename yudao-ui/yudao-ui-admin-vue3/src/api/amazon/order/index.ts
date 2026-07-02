import request from '@/config/axios'

// 订单管理 API
export const OrderApi = {
  // 获取订单分页列表
  getOrderPage: (params) => {
    return request.get({ url: '/amazon/order/page', params })
  },

  // 获取订单详情
  getOrder: (id) => {
    return request.get({ url: '/amazon/order/get', params: { id } })
  },

  // 手动同步订单
  syncOrders: (shopId) => {
    return request.post({ url: '/amazon/order/sync', params: { shopId } })
  }
}

// FBA 发货 API
export const FbaShipmentApi = {
  // 获取发货计划分页
  getShipmentPage: (params) => {
    return request.get({ url: '/amazon/fba-shipment/page', params })
  },

  // 创建发货计划
  createShipment: (data) => {
    return request.post({ url: '/amazon/fba-shipment/create', data })
  },

  // 更新发货计划
  updateShipment: (data) => {
    return request.put({ url: '/amazon/fba-shipment/update', data })
  },

  // 删除发货计划
  deleteShipment: (id) => {
    return request.delete({ url: '/amazon/fba-shipment/delete', params: { id } })
  },

  // 确认发货
  confirmShipment: (id) => {
    return request.post({ url: '/amazon/fba-shipment/confirm', params: { id } })
  }
}

// 订单统计 API
export const OrderStatsApi = {
  // 获取订单统计概览
  getOverview: (params) => {
    return request.get({ url: '/amazon/order-stats/overview', params })
  },

  // 获取销售趋势
  getSalesTrend: (params) => {
    return request.get({ url: '/amazon/order-stats/sales-trend', params })
  },

  // 获取热销商品
  getTopProducts: (params) => {
    return request.get({ url: '/amazon/order-stats/top-products', params })
  }
}
