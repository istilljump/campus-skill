<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-button type="primary" :icon="Plus" @click="openEdit()">新增类型</el-button>
      <span class="muted">平台按类型费率收取服务费，技能者实得 = 悬赏金额 ×（1 - 费率）</span>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column label="图标" width="70">
        <template #default="{ row }">
          <el-icon v-if="row.icon && row.icon.startsWith('el-')" :size="22"><component :is="row.icon.replace('el-', '')" /></el-icon>
          <span v-else>{{ row.icon || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="类型名称" width="140" />
      <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
      <el-table-column label="服务费率" width="100">
        <template #default="{ row }">{{ (Number(row.feeRate) * 100).toFixed(0) }}%</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 1" link type="warning" @click="toggleStatus(row, 0)">停用</el-button>
          <el-button v-else link type="success" @click="toggleStatus(row, 1)">启用</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑类型' : '新增类型'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="类型名称" prop="name">
          <el-input v-model="form.name" placeholder="如：帮我取快递" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="emoji 或图标名，如 📦" />
        </el-form-item>
        <el-form-item label="类型说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="服务费率" prop="feeRate">
          <el-input-number v-model="feeRatePercent" :min="0" :max="50" :step="1" />
          <span style="margin-left: 8px">%</span>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTypes, addType, updateType, setTypeStatus, deleteType } from '@/api'

const list = ref([])
const loading = ref(false)
const editVisible = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = reactive({ id: null, name: '', icon: '', description: '', sort: 0 })
const feeRatePercent = ref(10)
const rules = {
  name: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
}

const feeRate = computed(() => (feeRatePercent.value / 100).toFixed(2))

async function load() {
  loading.value = true
  try {
    list.value = await listTypes()
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  if (row) {
    Object.assign(form, { id: row.id, name: row.name, icon: row.icon || '', description: row.description || '', sort: row.sort || 0 })
    feeRatePercent.value = Math.round(Number(row.feeRate) * 100)
  } else {
    Object.assign(form, { id: null, name: '', icon: '', description: '', sort: 0 })
    feeRatePercent.value = 10
  }
  editVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...form, feeRate: feeRate.value }
    if (form.id) {
      await updateType(payload)
    } else {
      await addType(payload)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row, status) {
  await setTypeStatus(status, row.id)
  ElMessage.success('操作成功')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除类型「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteType(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
