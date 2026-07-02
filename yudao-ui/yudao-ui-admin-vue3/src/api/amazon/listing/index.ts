import request from '@/config/axios'

// 商品管理 API
export const ProductApi = {
  // 获取商品分页列表
  getProductPage: (params) => {
    return request.get({ url: '/amazon/listing/page', params })
  },

  // 获取商品详情
  getProduct: (id) => {
    return request.get({ url: '/amazon/listing/get', params: { id } })
  },

  // 创建商品
  createProduct: (data) => {
    return request.post({ url: '/amazon/listing/create', data })
  },

  // 更新商品
  updateProduct: (data) => {
    return request.put({ url: '/amazon/listing/update', data })
  },

  // 删除商品
  deleteProduct: (id) => {
    return request.delete({ url: '/amazon/listing/delete', params: { id } })
  },

  // 批量删除商品
  batchDeleteProducts: (ids) => {
    return request.delete({ url: '/amazon/listing/batch-delete', data: { ids } })
  }
}

// Listing 版本历史 API
export const ListingVersionApi = {
  // 获取版本历史
  getVersionList: (productId) => {
    return request.get({ url: '/amazon/listing/versions', params: { productId } })
  },

  // 回滚到指定版本
  rollbackToVersion: (versionId) => {
    return request.post({ url: '/amazon/listing/rollback', params: { versionId } })
  }
}

// 自动定价规则 API
export const AutoPriceApi = {
  // 获取定价规则列表
  getRulePage: (params) => {
    return request.get({ url: '/amazon/auto-price/page', params })
  },

  // 创建定价规则
  createRule: (data) => {
    return request.post({ url: '/amazon/auto-price/create', data })
  },

  // 更新定价规则
  updateRule: (data) => {
    return request.put({ url: '/amazon/auto-price/update', data })
  },

  // 删除定价规则
  deleteRule: (id) => {
    return request.delete({ url: '/amazon/auto-price/delete', params: { id } })
  },

  // 启用/禁用规则
  toggleRule: (id, enabled) => {
    return request.put({ url: '/amazon/auto-price/toggle', params: { id, enabled } })
  }
}
