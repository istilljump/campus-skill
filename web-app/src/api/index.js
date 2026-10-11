import request from '@/utils/request'

// ==================== 认证（A7 账号密码体系 + 体验码降级） ====================
export const accountLogin = (data) => request.post('/user/user/accountLogin', data)
export const register = (data) => request.post('/user/user/register', data)
export const guestLogin = (data) => request.post('/user/user/login', data)          // 体验码（联调降级）
export const changePassword = (data) => request.put('/user/user/password', data)
export const skillerAccountLogin = (data) => request.post('/skiller/auth/accountLogin', data)
export const skillerGuestLogin = (data) => request.post('/skiller/auth/login', data) // 体验码（联调降级）

// ==================== 用户端-个人 ====================
export const getProfile = () => request.get('/user/user/profile')
export const updateProfile = (data) => request.put('/user/user/profile', data)
export const applySkiller = (data) => request.post('/user/skiller/apply', data)

// ==================== 用户端-类目/地址 ====================
export const listCategories = () => request.get('/user/skillCategory/list')
export const listAddresses = () => request.get('/user/addressBook/list')
export const addAddress = (data) => request.post('/user/addressBook', data)
export const delAddress = (id) => request.delete('/user/addressBook', { params: { id } })

// ==================== 用户端-订单 ====================
export const submitOrder = (data) => request.post('/user/order/submit', data)
export const pageOrders = (params) => request.get('/user/order/page', { params })
export const getOrderDetail = (id) => request.get(`/user/order/detail/${id}`)
export const payOrder = (orderNumber) => request.post(`/user/order/pay/${orderNumber}`)
export const boostOrder = (data) => request.put('/user/order/boost', data)
export const cancelOrder = (data) => request.put('/user/order/cancel', data)
export const acceptOrder = (id) => request.put(`/user/order/accept/${id}`)
export const reworkOrder = (id, data) => request.put(`/user/order/rework/${id}`, data)
export const disputeOrder = (id, data) => request.post(`/user/order/dispute/${id}`, data)
export const getOrderReview = (id) => request.get(`/user/review/order/${id}`)
export const submitReview = (data) => request.post('/user/review/submit', data)

// ==================== 用户端-大厅/市场/预约 ====================
export const listHall = (params) => request.get('/user/hall/list', { params })
export const listMarket = (params) => request.get('/user/market', { params })
export const createBooking = (data) => request.post('/user/booking', data)
export const pageBookings = (params) => request.get('/user/booking/page', { params })
export const payBooking = (id) => request.post(`/user/booking/${id}/pay`)
export const cancelBooking = (id) => request.put(`/user/booking/${id}/cancel`)

// ==================== 用户端-钱包 ====================
export const getWallet = () => request.get('/user/wallet/balance')
export const pageTxs = (params) => request.get('/user/wallet/transactions', { params })
export const recharge = (data) => request.post('/user/wallet/recharge', data)

// ==================== 消息中心（用户侧 /skiller 侧共用结构） ====================
export const listMessages = () => request.get('/user/message/list')
export const countUnread = () => request.get('/user/message/unread')
export const readAllMessages = () => request.put('/user/message/readAll')

// ==================== 技能者端-个人中心/钱包 ====================
export const skCenter = () => request.get('/skiller/info/center')
export const skTrend = (days) => request.get('/skiller/info/trend', { params: { days } })
export const skWallet = () => request.get('/skiller/wallet/balance')
export const skPageTxs = (params) => request.get('/skiller/wallet/transactions', { params })
export const skPageWithdraws = (params) => request.get('/skiller/wallet/withdraw/page', { params })
export const skApplyWithdraw = (data) => request.post('/skiller/wallet/withdraw', data)
export const skSetWithdrawPwd = (password) => request.post('/skiller/wallet/password', null, { params: { password } })

// ==================== 技能者端-大厅/接单/交付 ====================
export const skHall = (params) => request.get('/skiller/orders/hall', { params })
export const skGrab = (data) => request.post('/skiller/orders/grab', data)
export const skPageOrders = (params) => request.get('/skiller/orders/page', { params })
export const skPickup = (id) => request.put(`/skiller/orders/pickup/${id}`)
export const skDeliver = (id, data) => request.put(`/skiller/orders/deliver/${id}`, data)
export const skRespondRework = (id) => request.post(`/skiller/orders/rework/${id}`)

// ==================== 技能者端-预约单 ====================
export const skPageBookings = (params) => request.get('/skiller/booking/page', { params })
export const skConfirmBooking = (id) => request.post(`/skiller/booking/${id}/confirm`)
export const skRejectBooking = (id) => request.post(`/skiller/booking/${id}/reject`)

// ==================== 技能者端-服务/作品/分类 ====================
export const skListServices = () => request.get('/skiller/service/list')
export const skAddService = (data) => request.post('/skiller/service', data)
export const skUpdateService = (id, data) => request.put(`/skiller/service/${id}`, data)
export const skToggleService = (id, status) => request.put(`/skiller/service/${id}/status/${status}`)
export const skListPortfolio = () => request.get('/skiller/portfolio')
export const skAddPortfolio = (data) => request.post('/skiller/portfolio', data)
export const skDelPortfolio = (id) => request.delete(`/skiller/portfolio/${id}`)
export const skListCategories = () => request.get('/user/skillCategory/list')
export const skHallCategories = () => request.get('/user/hall/skillCategory/list')

// ==================== 技能者端-消息 ====================
export const skListMessages = () => request.get('/skiller/message/list')
export const skCountUnread = () => request.get('/skiller/message/unread')
export const skReadAllMessages = () => request.put('/skiller/message/readAll')
