<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <span class="logo-badge"><el-icon :size="20"><Briefcase /></el-icon></span>
        <span class="logo-text">CampusSkill 门户</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <!-- 用户端导航（有 user token 即显示） -->
        <template v-if="store.hasUser">
          <el-menu-item index="/home"><el-icon><HomeFilled /></el-icon><span>概览</span></el-menu-item>
          <el-menu-item index="/hall"><el-icon><Compass /></el-icon><span>悬赏大厅</span></el-menu-item>
          <el-menu-item index="/market"><el-icon><ShoppingCart /></el-icon><span>技能市场</span></el-menu-item>
          <el-menu-item index="/submit"><el-icon><EditPen /></el-icon><span>发布悬赏</span></el-menu-item>
          <el-menu-item index="/orders"><el-icon><Tickets /></el-icon><span>我的订单</span></el-menu-item>
          <el-menu-item index="/wallet"><el-icon><Wallet /></el-icon><span>我的钱包</span></el-menu-item>
          <el-menu-item index="/messages"><el-icon><Bell /></el-icon><span>消息中心</span>
            <el-badge v-if="store.unread > 0 && !isSkArea" :value="store.unread" :max="99" class="nav-badge" /></el-menu-item>
          <el-menu-item index="/address"><el-icon><Location /></el-icon><span>地址管理</span></el-menu-item>
        </template>

        <!-- 技能者工作台（有 runner token 即显示） -->
        <template v-if="store.hasSkiller">
          <li class="group-title">技能者工作台</li>
          <el-menu-item index="/sk/orders"><el-icon><Opportunity /></el-icon><span>接单台</span></el-menu-item>
          <el-menu-item index="/sk/services"><el-icon><Goods /></el-icon><span>我的服务</span></el-menu-item>
          <el-menu-item index="/sk/portfolio"><el-icon><Picture /></el-icon><span>我的作品</span></el-menu-item>
          <el-menu-item index="/sk/center"><el-icon><User /></el-icon><span>技能者中心</span></el-menu-item>
          <el-menu-item index="/sk/messages"><el-icon><Bell /></el-icon><span>消息中心</span>
            <el-badge v-if="store.unread > 0 && isSkArea" :value="store.unread" :max="99" class="nav-badge" /></el-menu-item>
        </template>

        <!-- 引导：已登录用户端但还不是技能者 -->
        <template v-if="store.hasUser && !store.hasSkiller">
          <li class="group-title">更多</li>
          <el-menu-item index="/apply-skiller"><el-icon><Star /></el-icon><span>成为技能者</span></el-menu-item>
        </template>
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
          <el-tag v-if="isSkArea" type="warning" effect="plain" size="small">技能者身份</el-tag>
          <el-tag v-else-if="store.hasUser" type="primary" effect="plain" size="small">用户身份</el-tag>
          <el-dropdown @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="30" class="user-avatar">{{ initial }}</el-avatar>
              <span>{{ displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="store.hasUser" command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
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
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { skCenter } from '@/api'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')
const isSkArea = computed(() => route.meta.area === 'sk')
const displayName = computed(() =>
  isSkArea.value ? (store.center?.name || '技能者') : (store.profile?.name || '校园用户'))
const initial = computed(() => (displayName.value || 'C').slice(0, 1))

// 进入不同分区时拉取对应身份数据（未读数 + 顶栏展示名）并保持 WS 连接
watch(isSkArea, async (sk) => {
  store.loadUnread(sk ? 'sk' : 'user')
  if (sk && store.hasSkiller && !store.center?.name) {
    try { store.center = await skCenter() || {} } catch (e) {}
  }
}, { immediate: true })

onMounted(() => {
  if (store.hasUser) store.loadProfile()
  const token = store.hasSkiller && isSkArea.value ? store.runnerToken : store.userToken
  if (token) store.connectWs(token)
  store.loadUnread(isSkArea.value ? 'sk' : 'user')
})

async function onCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    await store.logout()
    ElMessage.success('已退出')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100%; }

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

.menu { border-right: none; flex: 1; background: transparent; overflow-y: auto; }

.menu :deep(.el-menu-item) {
  color: #61666d;
  margin: 2px 10px;
  border-radius: 10px;
  height: 44px;
  transition: background 0.16s ease, color 0.16s ease;
}
.menu :deep(.el-menu-item:hover) { background: #f6f7f8; color: #18191c; }
.menu :deep(.el-menu-item.is-active) {
  background: rgba(0, 174, 236, 0.1);
  color: #00aeec;
  box-shadow: inset 2px 0 0 #00aeec;
}

.group-title {
  list-style: none;
  padding: 14px 20px 4px;
  color: #9499a0;
  font-size: 12px;
  letter-spacing: 1px;
}

.nav-badge { margin-left: auto; margin-right: 8px; }

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
  box-shadow: 0 0 8px #2ac864;
}

.header {
  height: 60px;
  background: rgba(255, 255, 255, 0.85);
  border-bottom: 1px solid #e3e5e7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #18191c;
}

.header-title { font-size: 16px; font-weight: 600; }
.header-right { display: flex; align-items: center; gap: 12px; }

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #18191c;
}

.user-avatar { background: linear-gradient(135deg, #00aeec, #fb7299); }

.main { padding: 20px; background: transparent; }
</style>
