import request from './request'

// 历史客户风险画像
export const customerRiskApi = {
  page: (data) => request.post('/api/customer-risk/page', data),
  detail: (id) => request.post(`/api/customer-risk/detail/${id}`),
  create: (data) => request.post('/api/customer-risk/create', data),
  update: (id, data) => request.post(`/api/customer-risk/update/${id}`, data),
  remove: (id) => request.post(`/api/customer-risk/delete/${id}`)
}

// 投保申请
export const applicationApi = {
  page: (data) => request.post('/api/applications/page', data),
  pageJoined: (data) => request.post('/api/applications/page-joined', data),
  detail: (id) => request.post(`/api/applications/detail/${id}`),
  withDecision: (id) => request.post(`/api/applications/with-decision/${id}`),
  create: (data) => request.post('/api/applications/create', data),
  update: (id, data) => request.post(`/api/applications/update/${id}`, data),
  remove: (id) => request.post(`/api/applications/delete/${id}`)
}

// 核保决策结果
export const decisionApi = {
  page: (data) => request.post('/api/decisions/page', data),
  detail: (id) => request.post(`/api/decisions/detail/${id}`),
  byApplication: (id) => request.post(`/api/decisions/by-application/${id}`),
  create: (data) => request.post('/api/decisions/create', data),
  update: (id, data) => request.post(`/api/decisions/update/${id}`, data),
  remove: (id) => request.post(`/api/decisions/delete/${id}`),
  // 人工修整 / 分级审批 / 审计留痕
  adjust: (id, data) => request.post(`/api/decisions/adjust/${id}`, data),
  review: (id, data) => request.post(`/api/decisions/adjust/${id}/review`, data),
  audit: (id) => request.post(`/api/decisions/adjust/${id}/audit`),
  // 创新点③：AI 置信度智能路由分布统计
  confidenceStats: () => request.post('/api/decisions/confidence-stats')
}

// 创新点⑤：反馈驱动的规则优化闭环
export const ruleSuggestionApi = {
  fromFeedback: (feedbackId, data) => request.post(`/api/rule-suggestions/from-feedback/${feedbackId}`, data || {}),
  list: (data) => request.post('/api/rule-suggestions/list', data || {}),
  review: (id, data) => request.post(`/api/rule-suggestions/${id}/review`, data),
  stats: () => request.post('/api/rule-suggestions/stats')
}

// 风险计分规则 + 规则引擎
export const ruleApi = {
  list: () => request.post('/api/rules/list'),
  tryScore: (data) => request.post('/api/rules/try-score', data)
}

// 数据汇总统计
export const statApi = {
  overview: (data) => request.post('/api/stats/overview', data || {}),
  aggregate: (data) => request.post('/api/stats/aggregate', data)
}

// 核保预测
export const predictApi = {
  preview: (data) => request.post('/api/predict/preview', data),
  single: (id) => request.post(`/api/predict/single/${id}`),
  batch: (ids) => request.post('/api/predict/batch', { ids })
}

// 自然语言指令
export const nlApi = {
  parse: (text) => request.post('/api/nl/parse', { text }),
  // 自然语言统计（text-to-SQL：LLM 生成 SELECT 后执行），LLM 生成耗时较长需放宽超时
  stats: (text) => request.post('/api/nl/stats', { text }, { timeout: 120000 })
}

// 用户反馈
export const feedbackApi = {
  list: (data) => request.post('/api/feedback/list', data),
  stats: () => request.post('/api/feedback/stats'),
  create: (data) => request.post('/api/feedback/create', data),
  update: (id, data) => request.post(`/api/feedback/update/${id}`, data)
}
