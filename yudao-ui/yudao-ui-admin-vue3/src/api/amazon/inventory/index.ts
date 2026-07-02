import request from '@/config/axios'

// 库存管理 API
export const InventoryApi = {
  // 获取库存分页列表
  getInventoryPage: (params) => {
    return request.get({ url: '/amazon/inventory/page', params })
  },

  // 获取库存详情
  getInventory: (id) => {
    return request.get({ url: '/amazon/inventory/get', params: { id } })
  },

  // 同步库存
  syncInventory: (shopId) => {
    return request.post({ url: '/amazon/inventory/sync', params: { shopId } })
  }
}

// 库存预测 API
export const InventoryForecastApi = {
  // 获取库存预测分页
  getForecastPage: (params) => {
    return request.get({ url: '/amazon/inventory-forecast/page', params })
  },

  // 获取库存预测详情
  getForecast: (id) => {
    return request.get({ url: '/amazon/inventory-forecast/get', params: { id } })
  }
}

// 补货预警 API
export const ReplenishAlertApi = {
  // 获取补货预警分页
  getReplenishAlertPage: (params) => {
    return request.get({ url: '/amazon/replenish-alert/page', params })
  },

  // 创建补货预警
  createReplenishAlert: (data) => {
    return request.post({ url: '/amazon/replenish-alert/create', data })
  },

  // 更新补货预警
  updateReplenishAlert: (data) => {
    return request.put({ url: '/amazon/replenish-alert/update', data })
  },

  // 删除补货预警
  deleteReplenishAlert: (id) => {
    return request.delete({ url: '/amazon/replenish-alert/delete', params: { id } })
  }
}
