<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-input v-model="query.name" placeholder="姓名" clearable />
      <el-input v-model="query.campus" placeholder="校区" clearable />
      <el-select v-model="query.auditStatus" placeholder="认证状态" clearable style="width: 140px">
        <el-option v-for="(v, k) in AUDIT_STATUS" :key="k" :label="v.label" :value="Number(k)" />
      </el-select>
      <el-select v-model="query.status" placeholder="账号状态" clearable style="width: 140px">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="studentNo" label="学号" width="140" />
      <el-table-column prop="campus" label="校区" width="110" />
      <el-table-column prop="college" label="学院" width="140" />
      <el-table-column label="认证状态" width="100">
        <template #default="{ row }">
          <el-tag :type="AUDIT_STATUS[row.auditStatus]?.type">{{ AUDIT_STATUS[row.auditStatus]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="等级" width="70">
        <template #default="{ row }">
          <el-tag type="warning" effect="plain">Lv.{{ row.runnerLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="completedOrders" label="完成单数" width="90" />
      <el-table-column prop="score" label="评分" width="80" />
      <el-table-column label="账号状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageSkillers, setSkillerStatus } from '@/api'
import { AUDIT_STATUS } from '@/utils/dict'

const query = reactive({ page: 1, pageSize: 10, name: '', campus: '', auditStatus: null, status: null })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await pageSkillers(query)
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
  Object.assign(query, { page: 1, name: '', campus: '', auditStatus: null, status: null })
  load()
}

async function toggleStatus(row, status) {
  const action = status === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确定${action}技能者「${row.name}」吗？`, '提示', { type: 'warning' })
  await setSkillerStatus(status, row.id)
  ElMessage.success(`${action}成功`)
  load()
}

onMounted(load)
</script>
