import request from '@/config/axios'

// 店铺管理 API
export const ShopApi = {
  // 获取店铺分页列表
  getShopPage: (params) => {
    return request.get({ url: '/amazon/shop/page', params })
  },

  // 获取店铺详情
  getShop: (id) => {
    return request.get({ url: '/amazon/shop/get', params: { id } })
  },

  // 创建店铺
  createShop: (data) => {
    return request.post({ url: '/amazon/shop/create', data })
  },

  // 更新店铺
  updateShop: (data) => {
    return request.put({ url: '/amazon/shop/update', data })
  },

  // 删除店铺
  deleteShop: (id) => {
    return request.delete({ url: '/amazon/shop/delete', params: { id } })
  },

  // 测试店铺连接
  testConnection: (id) => {
    return request.post({ url: '/amazon/shop/test-connection', params: { id } })
  }
}

// API 监控 API
export const ApiMonitorApi = {
  // 获取 API 调用日志分页
  getApiLogPage: (params) => {
    return request.get({ url: '/amazon/api-monitor/log-page', params })
  },

  // 获取 API 调用统计
  getApiStats: (params) => {
    return request.get({ url: '/amazon/api-monitor/stats', params })
  }
}
