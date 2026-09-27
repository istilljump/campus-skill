<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-input v-model="query.name" placeholder="姓名 / 用户名" clearable />
      <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
      <el-button type="primary" :icon="Plus" @click="openEdit()">新增员工</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="name" label="姓名" width="110" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="性别" width="70">
        <template #default="{ row }">{{ row.sex === '1' ? '女' : '男' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 1" link type="danger" @click="toggleStatus(row, 0)">禁用</el-button>
          <el-button v-else link type="success" @click="toggleStatus(row, 1)">启用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pager">
      <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.pageSize" :page-sizes="[10, 20, 50]"
        @change="load" />
    </div>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑员工' : '新增员工'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.sex">
            <el-radio value="0">男</el-radio>
            <el-radio value="1">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idNumber" />
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
import { onMounted, reactive, ref } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageEmployees, addEmployee, updateEmployee, setEmployeeStatus } from '@/api'
import { fmtTime } from '@/utils/dict'

const query = reactive({ page: 1, pageSize: 10, name: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const editVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({ id: null, username: '', name: '', phone: '', sex: '0', idNumber: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const data = await pageEmployees(query)
    list.value = data.records || []
    total.value = Number(data.total) || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  load()
}

function onReset() {
  Object.assign(query, { page: 1, name: '' })
  load()
}

function openEdit(row) {
  if (row) {
    Object.assign(form, { id: row.id, username: row.username, name: row.name, phone: row.phone, sex: String(row.sex ?? '0'), idNumber: row.idNumber || '' })
  } else {
    Object.assign(form, { id: null, username: '', name: '', phone: '', sex: '0', idNumber: '' })
  }
  editVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await updateEmployee(form)
    } else {
      await addEmployee(form)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row, status) {
  const action = status === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确定${action}员工「${row.name}」吗？`, '提示', { type: 'warning' })
  await setEmployeeStatus(status, row.id)
  ElMessage.success(`${action}成功`)
  load()
}

onMounted(load)
</script>
