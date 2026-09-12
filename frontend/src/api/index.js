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
  remove: (id) => request.post(`/api/decisions/delete/${id}`)
}

// 风险计分规则 + 规则引擎
export const ruleApi = {
  list: () => request.post('/api/rules/list'),
  tryScore: (data) => request.post('/api/rules/try-score', data)
}

// 数据汇总统计
export const statApi = {
  overview: (data) => request.post('/api/stats/overview', data || {})
}

// 核保预测
export const predictApi = {
  preview: (data) => request.post('/api/predict/preview', data),
  single: (id) => request.post(`/api/predict/single/${id}`),
  batch: (ids) => request.post('/api/predict/batch', { ids })
}

// 自然语言指令
export const nlApi = {
  parse: (text) => request.post('/api/nl/parse', { text })
}
