import { createRouter, createWebHashHistory } from 'vue-router'
import { getUserToken, getRunnerToken } from '@/utils/request'

// 路由分区：meta.area = 'user' 用户端业务 | 'sk' 技能者端业务（导航分组与守卫依赖）
const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录' } },
  {
    path: '/',
    component: () => import('@/layout/WebLayout.vue'),
    redirect: '/home',
    children: [
      // ===== 用户端 =====
      { path: 'home', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { area: 'user', title: '概览' } },
      { path: 'hall', name: 'hall', component: () => import('@/views/HallView.vue'), meta: { area: 'user', title: '悬赏大厅' } },
      { path: 'market', name: 'market', component: () => import('@/views/MarketView.vue'), meta: { area: 'user', title: '技能市场' } },
      { path: 'submit', name: 'submit', component: () => import('@/views/SubmitOrderView.vue'), meta: { area: 'user', title: '发布悬赏' } },
      { path: 'orders', name: 'orders', component: () => import('@/views/OrderListView.vue'), meta: { area: 'user', title: '我的订单' } },
      { path: 'orders/:id', name: 'orderDetail', component: () => import('@/views/OrderDetailView.vue'), meta: { area: 'user', title: '订单详情' } },
      { path: 'wallet', name: 'wallet', component: () => import('@/views/WalletView.vue'), meta: { area: 'user', title: '我的钱包' } },
      { path: 'messages', name: 'messages', component: () => import('@/views/MessageView.vue'), meta: { area: 'user', title: '消息中心' } },
      { path: 'address', name: 'address', component: () => import('@/views/AddressView.vue'), meta: { area: 'user', title: '地址管理' } },
      { path: 'profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { area: 'user', title: '个人资料' } },
      { path: 'apply-skiller', name: 'applySkiller', component: () => import('@/views/ApplySkillerView.vue'), meta: { area: 'user', title: '成为技能者' } },
      // ===== 技能者端（需技能者 token） =====
      { path: 'sk/orders', name: 'skOrders', component: () => import('@/views/sk/SkOrdersView.vue'), meta: { area: 'sk', title: '接单台', requireSkiller: true } },
      { path: 'sk/services', name: 'skServices', component: () => import('@/views/sk/SkServicesView.vue'), meta: { area: 'sk', title: '我的服务', requireSkiller: true } },
      { path: 'sk/portfolio', name: 'skPortfolio', component: () => import('@/views/sk/SkPortfolioView.vue'), meta: { area: 'sk', title: '我的作品', requireSkiller: true } },
      { path: 'sk/center', name: 'skCenter', component: () => import('@/views/sk/SkCenterView.vue'), meta: { area: 'sk', title: '技能者中心', requireSkiller: true } },
      { path: 'sk/messages', name: 'skMessages', component: () => import('@/views/MessageView.vue'), meta: { area: 'sk', title: '消息中心', requireSkiller: true } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  // hash 模式：打包后可直接挂在 Spring Boot 静态资源目录下运行
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.path !== '/login') {
    if (to.meta.requireSkiller && !getRunnerToken()) return { path: '/login', query: { redirect: to.fullPath, role: 'skiller' } }
    if (!getUserToken() && !getRunnerToken()) return { path: '/login', query: { redirect: to.fullPath } }
  }
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · CampusSkill Web 门户` : 'CampusSkill Web 门户'
})

export default router
