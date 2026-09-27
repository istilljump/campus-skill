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

// ========== 跑腿员 ==========
export const pageRunners = (params) => request.get('/admin/runner/page', { params })
export const setRunnerStatus = (status, runnerId) => request.post(`/admin/runner/status/${status}`, null, { params: { runnerId } })

// ========== 认证审核 ==========
export const pageAudits = (params) => request.get('/admin/runnerAudit/page', { params })
export const processAudit = (data) => request.put('/admin/runnerAudit/process', data)

// ========== 订单 ==========
export const pageOrders = (params) => request.get('/admin/order/page', { params })
export const getOrderDetail = (id) => request.get(`/admin/order/detail/${id}`)

// ========== 订单类型 ==========
export const listTypes = () => request.get('/admin/errandType/list')
export const addType = (data) => request.post('/admin/errandType', data)
export const updateType = (data) => request.put('/admin/errandType', data)
export const setTypeStatus = (status, id) => request.put(`/admin/errandType/status/${status}`, null, { params: { id } })
export const deleteType = (id) => request.delete(`/admin/errandType/${id}`)

// ========== 提现 ==========
export const pageWithdraws = (params) => request.get('/admin/withdraw/page', { params })
export const processWithdraw = (data) => request.put('/admin/withdraw/process', data)

// ========== 营业状态 ==========
export const getShopStatus = () => request.get('/admin/shop/status')
export const setShopStatus = (status) => request.put(`/admin/shop/${status}`)

// ========== 统计 ==========
export const overviewStats = () => request.get('/admin/statistics/overview')
export const trendStats = (days = 7) => request.get('/admin/statistics/trend', { params: { days } })
export const typeRankStats = (limit = 5) => request.get('/admin/statistics/typeRank', { params: { limit } })
export const campusRankStats = () => request.get('/admin/statistics/campusRank')
