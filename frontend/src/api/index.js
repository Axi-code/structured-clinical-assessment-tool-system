import request from '../utils/request'

// 用户相关API
export const userApi = {
  login: (data) => request.post('/user/login', data),
  refresh: () => request.post('/user/refresh'),
  logout: () => request.post('/user/logout'),
  getCaptcha: () => request.get('/user/captcha'),
  register: (data) => request.post('/user/register', data),
  getUserInfo: () => request.get('/user/info'),
  getUserList: (params) => request.get('/user/list', { params }),
  addUser: (data) => request.post('/user/add', data),
  updateUser: (data) => request.put('/user/update', data),
  deleteUser: (id) => request.delete(`/user/delete/${id}`),
  updatePassword: (oldPassword, newPassword) => request.put('/user/password', null, {
    params: { oldPassword, newPassword }
  })
}

// 患者相关API
export const patientApi = {
  getPatientList: (params) => request.get('/patient/list', { params }),
  getPatient: (id) => request.get(`/patient/${id}`),
  getPatientForEdit: (id) => request.get(`/patient/${id}/edit`),
  addPatient: (data) => request.post('/patient/add', data),
  updatePatient: (data) => request.put('/patient/update', data),
  updateDiagnosis: (id, diagnosisId) => request.put(`/patient/${id}/diagnosis`, { diagnosisId }),
  deletePatient: (id) => request.delete(`/patient/delete/${id}`)
}

// 评估模板相关API
export const templateApi = {
  getTemplateList: (params) => request.get('/assessment-template/list', { params }),
  getTemplate: (id) => request.get(`/assessment-template/${id}`),
  getTemplateFields: (id) => request.get(`/assessment-template/${id}/fields`),
  addTemplate: (data) => request.post('/assessment-template/add', data),
  updateTemplate: (data) => request.put('/assessment-template/update', data),
  deleteTemplate: (id) => request.delete(`/assessment-template/delete/${id}`),
  updateTemplateStatus: (id, status) => request.put(`/assessment-template/${id}/status`, null, { params: { status } }),
  getTemplateVersions: (templateCode) => request.get(`/assessment-template/${templateCode}/versions`),
  createNewVersion: (id, data) => request.post(`/assessment-template/${id}/create-version`, data),
  addField: (data) => request.post('/assessment-template/field/add', data),
  updateField: (data) => request.put('/assessment-template/field/update', data),
  deleteField: (id) => request.delete(`/assessment-template/field/delete/${id}`)
}

// 评估记录相关API
export const assessmentApi = {
  createDraft: (data) => request.post('/assessment-record/draft', data),
  saveAssessment: (data) => request.post('/assessment-record/save', data),
  submitAssessment: (data) => request.post('/assessment-record/submit', data),
  getPatientHistory: (patientId) => request.get(`/assessment-record/history/${patientId}`),
  compareRecords: (recordIds) => request.post('/assessment-record/compare', recordIds)
}

// 对话式评估相关API
export const assessmentConversationApi = {
  generateTemplate: (data) => request.post('/assessment-conversation/generate-template', data),
  startConversation: (data) => request.post('/assessment-conversation/start', data),
  sendReply: (data) => request.post('/assessment-conversation/reply', data),
  calculateRealtime: (data) => request.post('/assessment-conversation/calculate-realtime', data),
  finalizeConversation: (data) => request.post('/assessment-conversation/finalize', data),
  recommendTemplate: (data) => request.post('/assessment-conversation/recommend-template', data)
}

// 报告相关API
export const reportApi = {
  generatePdf: (recordId, templateId) => {
    const params = templateId ? { templateId } : {}
    return request.get(`/report/pdf/${recordId}`, {
      params,
      responseType: 'blob'
    })
  },
  generateWord: (recordId, templateId) => {
    const params = templateId ? { templateId } : {}
    return request.get(`/report/word/${recordId}`, {
      params,
      responseType: 'blob'
    })
  },
  previewPdf: (recordId, templateId) => {
    const params = templateId ? { templateId } : {}
    return request.get(`/report/preview/pdf/${recordId}`, { params })
  },
  previewWord: (recordId, templateId) => {
    const params = templateId ? { templateId } : {}
    return request.get(`/report/preview/word/${recordId}`, { params })
  }
}

// 报告模板相关API
export const reportTemplateApi = {
  getTemplateList: (params) => request.get('/report-template/list', { params }),
  getByAssessmentTemplateId: (assessmentTemplateId) => request.get(`/report-template/assessment-template/${assessmentTemplateId}`),
  getTemplate: (id) => request.get(`/report-template/${id}`),
  createTemplate: (data) => request.post('/report-template/create', data),
  updateTemplate: (id, data) => request.put(`/report-template/${id}`, data),
  deleteTemplate: (id) => request.delete(`/report-template/${id}`),
  setDefaultTemplate: (id) => request.put(`/report-template/${id}/set-default`),
  updateTemplateStatus: (id, status) => request.put(`/report-template/${id}/status`, null, { params: { status } })
}

// 统计相关API
export const statisticsApi = {
  getDashboardStats: () => request.get('/statistics/dashboard'),
  statisticsByTime: (params) => request.get('/statistics/time', { params }),
  statisticsByDepartment: (params) => request.get('/statistics/department', { params }),
  statisticsByTemplate: (params) => request.get('/statistics/template', { params }),
  statisticsByRiskLevel: (params) => request.get('/statistics/risk-level', { params }),
  getIndicatorTrend: (params) => request.get('/statistics/indicator/trend', { params }),
  getIndicatorDistribution: (params) => request.get('/statistics/indicator/distribution', { params }),
  getStatisticsOverview: (params) => request.get('/statistics/overview', { params }),
  getRiskAlert: (params) => request.get('/statistics/risk-alert', { params })
}

// 评估规则相关API
export const ruleApi = {
  getRuleList: (params) => request.get('/assessment-rule/list', { params }),
  getRulesByTemplateId: (templateId) => request.get(`/assessment-rule/template/${templateId}`),
  getRule: (id) => request.get(`/assessment-rule/${id}`),
  addRule: (data) => request.post('/assessment-rule/create', data),
  updateRule: (id, data) => request.put(`/assessment-rule/${id}`, data),
  deleteRule: (id) => request.delete(`/assessment-rule/${id}`),
  updateRuleStatus: (id, status) => request.put(`/assessment-rule/${id}/status`, null, { params: { status } }),
  testRule: (data) => request.post('/assessment-rule/test', data),
  calculateRealtime: (data) => request.post('/assessment-rule/calculate-realtime', data)
}

// 科室相关API
export const departmentApi = {
  listAll: () => request.get('/department/list-all'),
  getPage: (params) => request.get('/department/page', { params }),
  add: (data) => request.post('/department/add', data),
  update: (data) => request.put('/department/update', data),
  delete: (id) => request.delete(`/department/delete/${id}`)
}

// 诊断相关API（按科室）
export const diagnosisApi = {
  listByDepartment: (departmentId) => request.get(`/diagnosis/by-department/${departmentId}`),
  getPage: (params) => request.get('/diagnosis/page', { params }),
  add: (data) => request.post('/diagnosis/add', data),
  update: (data) => request.put('/diagnosis/update', data),
  delete: (id) => request.delete(`/diagnosis/delete/${id}`)
}

// 操作日志
export const operationLogApi = {
  getLogPage: (params) => request.get('/operation-log/page', { params })
}

// 诊疗建议相关API
export const treatmentSuggestionApi = {
  generateSuggestion: (recordId) => request.post(`/treatment-suggestion/generate/${recordId}`),
  regenerateSuggestion: (recordId) => request.post(`/treatment-suggestion/regenerate/${recordId}`),
  getSuggestion: (id) => request.get(`/treatment-suggestion/${id}`),
  getSuggestionsByPatientId: (patientId) => request.get(`/treatment-suggestion/patient/${patientId}`),
  getSuggestionsByRecordId: (recordId) => request.get(`/treatment-suggestion/record/${recordId}`),
  getSuggestionPage: (params) => request.get('/treatment-suggestion/page', { params }),
  deleteSuggestion: (id) => request.delete(`/treatment-suggestion/${id}`)
}
