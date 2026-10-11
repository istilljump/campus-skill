<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-brand">
        <span class="login-badge"><el-icon :size="26"><Bicycle /></el-icon></span>
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
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="onSubmit">
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
  background:
    radial-gradient(1000px 480px at 15% -10%, rgba(59, 130, 246, 0.22), transparent 60%),
    radial-gradient(900px 460px at 105% 110%, rgba(124, 92, 255, 0.18), transparent 55%),
    #070b15;
}

/* 毛玻璃登录卡：与 H5 端登录页同构 */
.login-box {
  width: 400px;
  background: rgba(13, 20, 36, 0.72);
  -webkit-backdrop-filter: blur(16px) saturate(1.4);
  backdrop-filter: blur(16px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 40px 36px 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.45);
}

.login-brand {
  text-align: center;
  margin-bottom: 28px;
}

.login-badge {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: inline-grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #4f8cff, #7c5cff);
  box-shadow: 0 8px 24px -6px rgba(79, 140, 255, 0.6);
}

.login-brand h1 {
  margin: 12px 0 4px;
  font-size: 22px;
  color: #eef3fb;
}

.login-brand p {
  margin: 0;
  color: #a7b4c9;
  font-size: 14px;
}

.login-btn {
  width: 100%;
  margin-top: 4px;
}

.tip {
  margin: 14px 0 0;
  text-align: center;
  color: #64748b;
  font-size: 12px;
}
</style>
