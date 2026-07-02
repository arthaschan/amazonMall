import request from '@/config/axios'

// 评论管理 API
export const ReviewApi = {
  // 获取评论分页列表
  getReviewPage: (params) => {
    return request.get({ url: '/amazon/review/page', params })
  },

  // 获取评论详情
  getReview: (id) => {
    return request.get({ url: '/amazon/review/get', params: { id } })
  },

  // 同步评论
  syncReviews: (params) => {
    return request.post({ url: '/amazon/review/sync', params })
  }
}

// 评论预警 API
export const ReviewAlertApi = {
  // 获取预警规则分页
  getReviewAlertPage: (params) => {
    return request.get({ url: '/amazon/review/alert-page', params })
  },

  // 创建预警规则
  createReviewAlert: (data) => {
    return request.post({ url: '/amazon/review/alert/create', data })
  },

  // 更新预警规则
  updateReviewAlert: (data) => {
    return request.put({ url: '/amazon/review/alert/update', data })
  },

  // 删除预警规则
  deleteReviewAlert: (id) => {
    return request.delete({ url: '/amazon/review/alert/delete', params: { id } })
  }
}

// 客户消息模板 API
export const CustomerTemplateApi = {
  // 获取模板分页
  getCustomerTemplatePage: (params) => {
    return request.get({ url: '/amazon/review/template-page', params })
  },

  // 创建模板
  createCustomerTemplate: (data) => {
    return request.post({ url: '/amazon/review/template/create', data })
  },

  // 更新模板
  updateCustomerTemplate: (data) => {
    return request.put({ url: '/amazon/review/template/update', data })
  },

  // 删除模板
  deleteCustomerTemplate: (id) => {
    return request.delete({ url: '/amazon/review/template/delete', params: { id } })
  }
}

// AI 分析 API
export const AiAnalysisApi = {
  // 获取 AI 分析结果
  getAiAnalysis: (reviewId) => {
    return request.get({ url: '/amazon/review/ai-analysis', params: { reviewId } })
  }
}
