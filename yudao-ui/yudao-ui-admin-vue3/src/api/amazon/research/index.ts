import request from '@/config/axios'

// 利基市场分析 API
export const NicheApi = {
  // 获取利基分页
  getNichePage: (params) => {
    return request.get({ url: '/amazon/niche/page', params })
  },

  // 获取利基详情
  getNiche: (id) => {
    return request.get({ url: '/amazon/niche/get', params: { id } })
  },

  // 删除利基
  deleteNiche: (id) => {
    return request.delete({ url: '/amazon/niche/delete', params: { id } })
  }
}

// 选品机会 API
export const ProductOpportunityApi = {
  // 获取选品机会分页
  getProductOpportunityPage: (params) => {
    return request.get({ url: '/amazon/product-opportunity/page', params })
  },

  // 获取选品机会详情
  getProductOpportunity: (id) => {
    return request.get({ url: '/amazon/product-opportunity/get', params: { id } })
  }
}

// BSR 分析 API
export const BsrApi = {
  // 获取 BSR 数据
  getBsrData: (params) => {
    return request.get({ url: '/amazon/bsr/data', params })
  }
}

// 财务预测 API
export const FinancialApi = {
  // 获取财务预测
  getFinancialProjection: (params) => {
    return request.get({ url: '/amazon/financial/projection', params })
  }
}

// 关键词研究 API
export const KeywordResearchApi = {
  // 获取关键词研究数据
  getKeywordResearch: (params) => {
    return request.get({ url: '/amazon/keyword-research/data', params })
  }
}

// 供应商匹配 API
export const SupplierMatchApi = {
  // 获取供应商匹配数据
  getSupplierMatch: (params) => {
    return request.get({ url: '/amazon/supplier-match/data', params })
  }
}
