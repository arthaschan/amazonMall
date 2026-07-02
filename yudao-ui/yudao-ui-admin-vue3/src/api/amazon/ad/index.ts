import request from '@/config/axios'

// 广告活动 API
export const AdCampaignApi = {
  // 获取广告活动分页列表
  getAdCampaignPage: (params) => {
    return request.get({ url: '/amazon/ad/campaign-page', params })
  },

  // 获取广告活动详情
  getAdCampaign: (id) => {
    return request.get({ url: '/amazon/ad/campaign/get', params: { id } })
  },

  // 创建广告活动
  createAdCampaign: (data) => {
    return request.post({ url: '/amazon/ad/campaign/create', data })
  },

  // 更新广告活动
  updateAdCampaign: (data) => {
    return request.put({ url: '/amazon/ad/campaign/update', data })
  },

  // 删除广告活动
  deleteAdCampaign: (id) => {
    return request.delete({ url: '/amazon/ad/campaign/delete', params: { id } })
  },

  // 启用/暂停广告活动
  toggleCampaign: (id, state) => {
    return request.put({ url: '/amazon/ad/campaign/toggle', params: { id, state } })
  }
}

// 广告报告 API
export const AdReportApi = {
  // 获取广告报告分页
  getAdReportPage: (params) => {
    return request.get({ url: '/amazon/ad/report-page', params })
  },

  // 获取广告报告汇总
  getAdReportSummary: (params) => {
    return request.get({ url: '/amazon/ad/report-summary', params })
  }
}

// 广告搜索词 API
export const AdSearchTermApi = {
  // 获取搜索词分页
  getAdSearchTermPage: (params) => {
    return request.get({ url: '/amazon/ad/search-term-page', params })
  },

  // 添加否定关键词
  addNegativeKeyword: (data) => {
    return request.post({ url: '/amazon/ad/negative-keyword', data })
  }
}

// 广告规则 API
export const AdRuleApi = {
  // 获取广告规则分页
  getAdRulePage: (params) => {
    return request.get({ url: '/amazon/ad/rule-page', params })
  },

  // 创建广告规则
  createAdRule: (data) => {
    return request.post({ url: '/amazon/ad/rule/create', data })
  },

  // 更新广告规则
  updateAdRule: (data) => {
    return request.put({ url: '/amazon/ad/rule/update', data })
  },

  // 删除广告规则
  deleteAdRule: (id) => {
    return request.delete({ url: '/amazon/ad/rule/delete', params: { id } })
  }
}
