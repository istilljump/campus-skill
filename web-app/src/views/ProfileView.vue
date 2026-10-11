<template>
  <div class="profile-layout">
    <div class="page-card">
      <div class="card-title"><el-icon><User /></el-icon><span>个人资料</span></div>
      <el-form :model="form" label-position="top" style="max-width:420px">
        <el-form-item label="昵称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="学号"><el-input v-model="form.studentNo" /></el-form-item>
        <el-form-item label="校区"><el-input v-model="form.campus" /></el-form-item>
        <el-button type="primary" style="width:100%" :loading="busy" @click="onSave">保存资料</el-button>
      </el-form>
    </div>

    <div class="side-col">
      <div class="page-card">
        <div class="card-title"><el-icon><Medal /></el-icon><span>账号信息</span></div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户 ID">{{ store.profile.id }}</el-descriptions-item>
          <el-descriptions-item label="登录账号">{{ store.profile.username || '（体验账号，未设置）' }}</el-descriptions-item>
          <el-descriptions-item label="信用分">{{ store.profile.creditScore ?? 100 }}</el-descriptions-item>
          <el-descriptions-item label="累计发单">{{ store.profile.publishOrderCount ?? 0 }} 单</el-descriptions-item>
          <el-descriptions-item label="校区">{{ store.profile.campus || '未填写' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="page-card">
        <div class="card-title"><el-icon><Lock /></el-icon><span>修改密码</span></div>
        <el-form label-position="top" style="max-width:420px">
          <el-form-item label="原密码（首次设置可不填）"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码（6-32位）"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
          <el-form-item label="确认新密码"><el-input v-model="pwdForm.confirm" type="password" show-password /></el-form-item>
          <el-button type="primary" style="width:100%" :loading="busy" @click="onChangePwd">更新密码</el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updateProfile } from '@/api'

const store = useUserStore()
const busy = ref(false)
const form = reactive({ name: '', phone: '', studentNo: '', campus: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function onSave() {
  busy.value = true
  try {
    await updateProfile({ ...form })
    ElMessage.success('资料已更新')
    store.loadProfile()
  } finally { busy.value = false }
}

async function onChangePwd() {
  if (pwdForm.newPassword !== pwdForm.confirm) return ElMessage.warning('两次输入的新密码不一致')
  busy.value = true
  try {
    await store.changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码已更新')
    Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirm: '' })
  } finally { busy.value = false }
}

onMounted(async () => {
  await store.loadProfile()
  Object.assign(form, {
    name: store.profile.name, phone: store.profile.phone,
    studentNo: store.profile.studentNo, campus: store.profile.campus,
  })
})
</script>

<style scoped>
.profile-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
@media (max-width: 1000px) { .profile-layout { grid-template-columns: 1fr; } }
.side-col { display: grid; gap: 16px; }
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; margin-bottom: 12px; color: #e6edf7; }
</style>
