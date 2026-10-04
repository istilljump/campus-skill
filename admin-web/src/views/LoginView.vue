<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-brand">
        <el-icon :size="40" color="#2563eb"><Bicycle /></el-icon>
        <h1>CampusSkill 技能工坊</h1>
        <p>管理端控制台</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="onSubmit">
          登 录
        </el-button>
        <p class="tip">默认账号：admin / 123456</p>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 55%, #3b82f6 100%);
}

.login-box {
  width: 380px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}

.login-brand {
  text-align: center;
  margin-bottom: 28px;
}

.login-brand h1 {
  margin: 10px 0 4px;
  font-size: 22px;
}

.login-brand p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.tip {
  margin: 14px 0 0;
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
}
</style>
