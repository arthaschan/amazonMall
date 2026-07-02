import request from '@/config/axios'

// 仪表盘 API
export const DashboardApi = {
  // 获取仪表盘指标
  getDashboardMetrics: (params) => {
    return request.get({ url: '/amazon/report/dashboard-metrics', params })
  },

  // 获取仪表盘概览
  getDashboardOverview: (params) => {
    return request.get({ url: '/amazon/report/dashboard-overview', params })
  }
}

// 周报 API
export const WeeklyReportApi = {
  // 获取周报分页
  getWeeklyReportPage: (params) => {
    return request.get({ url: '/amazon/report/weekly-page', params })
  },

  // 获取周报详情
  getWeeklyReport: (id) => {
    return request.get({ url: '/amazon/report/weekly/get', params: { id } })
  },

  // 生成周报
  generateWeeklyReport: (data) => {
    return request.post({ url: '/amazon/report/weekly/generate', data })
  }
}

// AI 对话 API
export const AiChatApi = {
  // 发送消息
  sendAiChat: (data) => {
    return request.post({ url: '/amazon/report/ai-chat/send', data })
  },

  // 获取聊天历史
  getAiChatHistory: (params) => {
    return request.get({ url: '/amazon/report/ai-chat/history', params })
  }
}
