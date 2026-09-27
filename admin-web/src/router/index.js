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
      { path: 'runners', name: 'runners', component: () => import('@/views/runner/RunnerListView.vue'), meta: { title: '跑腿员管理' } },
      { path: 'audits', name: 'audits', component: () => import('@/views/runner/AuditListView.vue'), meta: { title: '认证审核' } },
      { path: 'withdraws', name: 'withdraws', component: () => import('@/views/finance/WithdrawListView.vue'), meta: { title: '提现审核' } },
      { path: 'types', name: 'types', component: () => import('@/views/type/TypeListView.vue'), meta: { title: '订单类型' } },
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
  document.title = to.meta.title ? `${to.meta.title} · 校园跑腿管理端` : '校园跑腿管理端'
})

export default router
