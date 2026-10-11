<template>
  <div class="page-card" style="max-width:640px">
    <div class="card-title"><el-icon><Star /></el-icon><span>技能者入驻申请</span></div>
    <p class="muted">提交后由平台审核，通过后即可在 Web 门户或技能者端 H5 登录接单；作品认证可提升定级（C1/C2/C3）与可接悬赏区间。</p>
    <el-form :model="form" label-position="top" style="max-width:420px">
      <el-form-item label="真实姓名"><el-input v-model="form.realName" placeholder="与学籍信息一致" /></el-form-item>
      <el-form-item label="学号"><el-input v-model="form.studentNo" /></el-form-item>
      <el-form-item label="校区"><el-input v-model="form.campus" /></el-form-item>
      <el-form-item label="学院"><el-input v-model="form.college" /></el-form-item>
      <el-button type="primary" style="width:100%" :loading="busy" @click="onSubmit">提交入驻申请</el-button>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { applySkiller } from '@/api'

const busy = ref(false)
const form = reactive({ realName: '', studentNo: '', campus: '', college: '' })

async function onSubmit() {
  if (!form.realName || !form.studentNo) return ElMessage.warning('请填写真实姓名与学号')
  busy.value = true
  try {
    await applySkiller({ ...form })
    ElMessage.success('申请已提交，等待平台审核')
  } finally { busy.value = false }
}
</script>

<style scoped>
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; margin-bottom: 12px; color: #e6edf7; }
</style>
