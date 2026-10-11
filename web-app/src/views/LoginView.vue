<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-brand">
        <span class="login-badge"><el-icon :size="26"><Briefcase /></el-icon></span>
        <h1>CampusSkill 校园技能工坊</h1>
        <p>桌面端 Web 门户 · 用户与技能者一体</p>
      </div>

      <el-tabs v-model="role" stretch class="role-tabs">
        <el-tab-pane label="用户登录" name="user" />
        <el-tab-pane label="技能者登录" name="skiller" />
      </el-tabs>

      <!-- 账号密码登录 / 注册 -->
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit" v-if="mode !== 'guest'">
        <template v-if="mode === 'account'">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="onSubmit">
            {{ role === 'skiller' ? '以技能者身份登录' : '登 录' }}
          </el-button>
          <div class="link-row">
            <el-link type="primary" :underline="false" @click="mode = 'register'" v-if="role === 'user'">没有账号？立即注册</el-link>
            <el-link type="info" :underline="false" @click="mode = 'guest'">体验登录（联调）</el-link>
          </div>
          <p class="tip" v-if="role === 'skiller'">技能者与用户端共用账号，需先通过入驻审核（用户端注册 → 成为技能者）</p>
        </template>

        <template v-else>
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名（4-20位字母/数字/下划线）" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="密码（6-32位）" :prefix-icon="Lock" />
          </el-form-item>
          <el-form-item prop="confirm">
            <el-input v-model="form.confirm" type="password" show-password placeholder="确认密码" :prefix-icon="Lock" />
          </el-form-item>
          <el-form-item prop="name">
            <el-input v-model="form.name" placeholder="昵称（选填）" :prefix-icon="Postcard" />
          </el-form-item>
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="onSubmit">注 册</el-button>
          <div class="link-row">
            <el-link type="primary" :underline="false" @click="mode = 'account'">‹ 返回账号登录</el-link>
          </div>
        </template>
      </el-form>

      <!-- 体验码登录（联调降级，与微信登录未配置 appid 的行为一致） -->
      <template v-else>
        <el-form size="large" @keyup.enter="onSubmit">
          <el-form-item>
            <el-input v-model="form.guestCode" placeholder="输入任意体验码即可登录（联调模式）" :prefix-icon="Key">
              <template #append><el-button @click="onSubmit">登录</el-button></template>
            </el-input>
          </el-form-item>
          <div class="link-row">
            <el-link type="primary" :underline="false" @click="mode = 'account'">‹ 返回账号登录</el-link>
          </div>
        </el-form>
      </template>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, Postcard, Briefcase } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const formRef = ref()
const loading = ref(false)
//role：登录身份；mode：account 账号登录 | register 注册 | guest 体验码
const role = ref(route.query.role === 'skiller' ? 'skiller' : 'user')
const mode = ref('account')
const form = reactive({ username: '', password: '', confirm: '', name: '', guestCode: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  loading.value = true
  try {
    if (mode.value === 'guest') {
      if (!form.guestCode) return ElMessage.warning('请输入体验码')
      if (role.value === 'skiller') await store.guestLoginAsSkiller(form.guestCode)
      else await store.guestLoginAsUser(form.guestCode)
      ElMessage.success('体验登录成功')
    } else if (mode.value === 'register') {
      if (form.password !== form.confirm) return ElMessage.warning('两次输入的密码不一致')
      await store.registerAsUser({ username: form.username, password: form.password, name: form.name })
      ElMessage.success('注册成功，已自动登录')
    } else {
      if (role.value === 'skiller') await store.loginAsSkiller({ username: form.username, password: form.password })
      else await store.loginAsUser({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
    }
    router.push(route.query.redirect || (role.value === 'skiller' ? '/sk/center' : '/home'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
}

.login-box {
  width: 430px;
  background: #ffffff;
  border: 1px solid #e3e5e7;
  border-radius: 20px;
  padding: 36px 36px 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
}

.login-brand {
  text-align: center;
  margin-bottom: 16px;
}

.login-badge {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: inline-grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #00aeec, #fb7299);
  box-shadow: 0 8px 24px -6px rgba(0, 174, 236, 0.35);
}

.login-brand h1 {
  margin: 12px 0 4px;
  font-size: 22px;
  color: #18191c;
}

.login-brand p {
  margin: 0;
  color: #61666d;
  font-size: 13px;
}

.role-tabs {
  margin-bottom: 8px;
}

.submit-btn {
  width: 100%;
}

.link-row {
  margin-top: 14px;
  display: flex;
  justify-content: space-between;
}

.tip {
  margin: 12px 0 0;
  color: #9499a0;
  font-size: 12px;
  text-align: center;
}
</style>
