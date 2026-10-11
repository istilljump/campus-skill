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
    radial-gradient(1000px 480px at 15% -10%, rgba(0, 174, 236, 0.12), transparent 60%),
    radial-gradient(900px 460px at 105% 110%, rgba(251, 114, 153, 0.08), transparent 55%),
    #f1f2f3;
}

/* 亮色登录卡：白底半透明 + 细边框 */
.login-box {
  width: 400px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #e3e5e7;
  border-radius: 20px;
  padding: 40px 36px 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
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
  font-size: 14px;
}

.login-btn {
  width: 100%;
  margin-top: 4px;
}

.tip {
  margin: 14px 0 0;
  text-align: center;
  color: #9499a0;
  font-size: 12px;
}
</style>
