<template>
  <div class="address-layout">
    <!-- 已有地址 -->
    <div class="page-card">
      <div class="card-title"><el-icon><Location /></el-icon><span>我的地址</span></div>
      <el-table :data="addresses" v-loading="loading" stripe>
        <el-table-column label="地址" min-width="220">
          <template #default="{ row }">
            {{ row.campus }} {{ row.building }} {{ row.room }}
            <el-tag v-if="row.isDefault === 1" size="small" type="primary" style="margin-left:6px">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="consignee" label="联系人" width="110" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="onDel(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="还没有收货地址" :image-size="72" /></template>
      </el-table>
    </div>

    <!-- 新增地址 -->
    <div class="page-card">
      <div class="card-title"><el-icon><Plus /></el-icon><span>新增地址</span></div>
      <el-form :model="form" label-position="top">
        <el-form-item label="联系人"><el-input v-model="form.consignee" placeholder="姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="手机号" /></el-form-item>
        <el-form-item label="校区"><el-input v-model="form.campus" placeholder="如：主校区" /></el-form-item>
        <el-form-item label="楼栋"><el-input v-model="form.building" placeholder="如：3号楼" /></el-form-item>
        <el-form-item label="房间号"><el-input v-model="form.room" placeholder="如：502" /></el-form-item>
        <el-button type="primary" style="width:100%" :loading="busy" @click="onSave">保存地址</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAddresses, addAddress, delAddress } from '@/api'

const addresses = ref([])
const loading = ref(false)
const busy = ref(false)
const form = reactive({ consignee: '', phone: '', campus: '', building: '', room: '' })

async function load() {
  loading.value = true
  try {
    addresses.value = await listAddresses() || []
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (!form.consignee || !form.phone) return ElMessage.warning('请填写联系人和手机号')
  busy.value = true
  try {
    await addAddress({ ...form, sex: '1', isDefault: addresses.value.length ? 0 : 1 })
    ElMessage.success('已保存')
    Object.assign(form, { consignee: '', phone: '', campus: '', building: '', room: '' })
    load()
  } finally { busy.value = false }
}

async function onDel(row) {
  await ElMessageBox.confirm('确定删除该地址吗？', '删除地址', { type: 'warning' })
  await delAddress(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.address-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 1fr);
  gap: 16px;
  align-items: start;
}
@media (max-width: 1000px) { .address-layout { grid-template-columns: 1fr; } }
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; margin-bottom: 12px; color: #e6edf7; }
</style>
