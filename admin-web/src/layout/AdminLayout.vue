<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <span class="logo-badge"><el-icon :size="20"><Bicycle /></el-icon></span>
        <span class="logo-text">CampusSkill 管理端</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item v-for="item in topMenus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
        <el-menu-item-group v-for="g in menuGroups" :key="g.label">
          <template #title><span class="group-title">{{ g.label }}</span></template>
          <el-menu-item v-for="item in g.items" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
      <div class="aside-footer">
        <span class="status-dot"></span>
        <span>平台运行中</span>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-right">
          <el-tooltip content="数据大屏" placement="bottom">
            <el-button circle text @click="$router.push('/dashboard')">
              <el-icon><DataBoard /></el-icon>
            </el-button>
          </el-tooltip>
          <el-dropdown @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="30" class="user-avatar">{{ initial }}</el-avatar>
              <span>{{ userStore.name || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 顶层数据大屏 + 三个业务分组的菜单结构
const topMenus = [
  { path: '/dashboard', title: '数据大屏', icon: 'DataBoard' },
]

const menuGroups = [
  {
    label: '业务管理',
    items: [
      { path: '/orders', title: '订单管理', icon: 'List' },
      { path: '/bookings', title: '预约管理', icon: 'Calendar' },
      { path: '/runners', title: '技能者管理', icon: 'Avatar' },
      { path: '/users', title: '用户管理', icon: 'User' },
      { path: '/credit-rank', title: '信用榜单', icon: 'TrophyBase' },
    ],
  },
  {
    label: '审核与仲裁',
    items: [
      { path: '/audits', title: '技能者认证', icon: 'Stamp' },
      { path: '/portfolio-audits', title: '作品审核', icon: 'Picture' },
      { path: '/disputes', title: '仲裁工单', icon: 'ScaleToOriginal' },
      { path: '/withdraws', title: '提现审核', icon: 'Money' },
    ],
  },
  {
    label: '系统配置',
    items: [
      { path: '/types', title: '技能类目', icon: 'Grid' },
      { path: '/announcements', title: '公告管理', icon: 'Bell' },
      { path: '/employees', title: '员工管理', icon: 'Setting' },
      { path: '/shop', title: '营业设置', icon: 'Shop' },
    ],
  },
]

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')
const initial = computed(() => (userStore.name || 'A').slice(0, 1).toUpperCase())

async function onCommand(cmd) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    await userStore.logout()
    ElMessage.success('已退出')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

/* 亮色侧边栏：白底 + 细边框 */
.aside {
  background: #ffffff;
  border-right: 1px solid #e3e5e7;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #18191c;
  font-weight: 600;
  font-size: 15px;
  border-bottom: 1px solid #f1f2f3;
}

.logo-badge {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #00aeec, #fb7299);
  box-shadow: 0 4px 14px -4px rgba(0, 174, 236, 0.4);
}

.menu {
  border-right: none;
  flex: 1;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  color: #61666d;
  margin: 2px 10px;
  border-radius: 10px;
  height: 44px;
  transition: background 0.16s ease, color 0.16s ease;
}

.menu :deep(.el-menu-item:hover) {
  background: #f6f7f8;
  color: #18191c;
}

.menu :deep(.el-menu-item.is-active) {
  background: rgba(0, 174, 236, 0.1);
  color: #00aeec;
  box-shadow: inset 2px 0 0 #00aeec;
}

.menu :deep(.el-menu-item-group__title) {
  padding: 12px 20px 4px;
  color: #9499a0;
  font-size: 12px;
}

.group-title {
  font-size: 12px;
  letter-spacing: 1px;
}

.aside-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  color: #9499a0;
  font-size: 12px;
  border-top: 1px solid #f1f2f3;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2ac864;
  box-shadow: 0 0 8px rgba(42, 200, 100, 0.5);
}

/* 亮色顶栏：半透明白 + 细边框 */
.header {
  height: 60px;
  background: rgba(255, 255, 255, 0.85);
  border-bottom: 1px solid #e3e5e7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #18191c;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #18191c;
}

.user-avatar {
  background: linear-gradient(135deg, #00aeec, #fb7299);
}

.main {
  padding: 20px;
  background: transparent;
}
</style>
