import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '@/utils/request'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录' } },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '数据大屏' } },
      { path: 'orders', name: 'orders', component: () => import('@/views/order/OrderListView.vue'), meta: { title: '订单管理' } },
      { path: 'runners', name: 'runners', component: () => import('@/views/runner/RunnerListView.vue'), meta: { title: '技能者管理' } },
      { path: 'audits', name: 'audits', component: () => import('@/views/runner/AuditListView.vue'), meta: { title: '技能者认证' } },
      { path: 'withdraws', name: 'withdraws', component: () => import('@/views/finance/WithdrawListView.vue'), meta: { title: '提现审核' } },
      { path: 'types', name: 'types', component: () => import('@/views/type/TypeListView.vue'), meta: { title: '技能类目' } },
      { path: 'portfolio-audits', name: 'portfolio-audits', component: () => import('@/views/skill/PortfolioAuditView.vue'), meta: { title: '作品审核' } },
      { path: 'disputes', name: 'disputes', component: () => import('@/views/skill/DisputeView.vue'), meta: { title: '仲裁工单' } },
      { path: 'credit-rank', name: 'credit-rank', component: () => import('@/views/skill/CreditRankView.vue'), meta: { title: '信用榜单' } },
      { path: 'users', name: 'users', component: () => import('@/views/user/UserListView.vue'), meta: { title: '用户管理' } },
      { path: 'employees', name: 'employees', component: () => import('@/views/employee/EmployeeListView.vue'), meta: { title: '员工管理' } },
      { path: 'shop', name: 'shop', component: () => import('@/views/shop/ShopView.vue'), meta: { title: '营业设置' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  // hash 模式：打包后可直接挂在任意静态目录（含 Spring Boot 静态资源）下运行
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.path !== '/login' && !getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · CampusSkill 技能工坊管理端` : 'CampusSkill 技能工坊管理端'
})

export default router
