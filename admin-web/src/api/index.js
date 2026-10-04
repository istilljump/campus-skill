import request from '@/utils/request'

// ========== 登录 / 员工 ==========
export const login = (data) => request.post('/admin/employee/login', data)
export const logout = () => request.post('/admin/employee/logout')
export const pageEmployees = (params) => request.get('/admin/employee/page', { params })
export const addEmployee = (data) => request.post('/admin/employee', data)
export const updateEmployee = (data) => request.put('/admin/employee', data)
export const getEmployee = (id) => request.get(`/admin/employee/${id}`)
export const setEmployeeStatus = (status, id) => request.post(`/admin/employee/status/${status}`, null, { params: { id } })

// ========== 用户 ==========
export const pageUsers = (params) => request.get('/admin/user/page', { params })

// ========== 技能者 ==========
export const pageSkillers = (params) => request.get('/admin/skiller/page', { params })
export const setSkillerStatus = (status, skillerId) => request.post(`/admin/skiller/status/${status}`, null, { params: { skillerId } })

// ========== 技能者认证 ==========
export const pageAudits = (params) => request.get('/admin/skillerAudit/page', { params })
export const processAudit = (data) => request.put('/admin/skillerAudit/process', data)

// ========== 订单 ==========
export const pageOrders = (params) => request.get('/admin/order/page', { params })
export const getOrderDetail = (id) => request.get(`/admin/order/detail/${id}`)

// ========== 技能类目 ==========
export const listTypes = () => request.get('/admin/skillCategory/list')
export const addType = (data) => request.post('/admin/skillCategory', data)
export const updateType = (data) => request.put('/admin/skillCategory', data)
export const setTypeStatus = (status, id) => request.put(`/admin/skillCategory/status/${status}`, null, { params: { id } })
export const deleteType = (id) => request.delete(`/admin/skillCategory/${id}`)

// ========== 提现 ==========
export const pageWithdraws = (params) => request.get('/admin/withdraw/page', { params })
export const processWithdraw = (data) => request.put('/admin/withdraw/process', data)

// ========== 作品审核 ==========
export const pagePortfolioAudit = (params) => request.get('/admin/portfolioAudit/page', { params })
export const processPortfolioAudit = (data) => request.put('/admin/portfolioAudit/process', data)

// ========== 仲裁工单 ==========
export const pageDisputes = (params) => request.get('/admin/dispute/page', { params })
export const verdictDispute = (id, data) => request.put(`/admin/dispute/${id}/verdict`, data)

// ========== 信用榜单 ==========
export const creditRank = (limit = 10) => request.get('/admin/credit/rank', { params: { limit } })

// ========== 营业状态 ==========
export const getShopStatus = () => request.get('/admin/shop/status')
export const setShopStatus = (status) => request.put(`/admin/shop/${status}`)

// ========== 统计 ==========
export const overviewStats = () => request.get('/admin/statistics/overview')
export const trendStats = (days = 7) => request.get('/admin/statistics/trend', { params: { days } })
export const typeRankStats = (limit = 5) => request.get('/admin/statistics/typeRank', { params: { limit } })
export const campusRankStats = () => request.get('/admin/statistics/campusRank')
