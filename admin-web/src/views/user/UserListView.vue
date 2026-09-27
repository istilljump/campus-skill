<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-input v-model="query.name" placeholder="用户名" clearable />
      <el-input v-model="query.phone" placeholder="手机号" clearable />
      <el-input v-model="query.campus" placeholder="校区" clearable />
      <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="用户名" width="130">
        <template #default="{ row }">{{ row.name || '微信用户' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="studentNo" label="学号" width="140" />
      <el-table-column prop="campus" label="校区" width="110" />
      <el-table-column label="信用分" width="100">
        <template #default="{ row }">
          <el-tag :type="creditType(row.creditScore)">{{ row.creditScore }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishOrderCount" label="累计发单" width="90" />
      <el-table-column label="注册时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
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
import { pageUsers } from '@/api'
import { fmtTime } from '@/utils/dict'

const query = reactive({ page: 1, pageSize: 10, name: '', phone: '', campus: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)

function creditType(score) {
  if (score >= 90) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

async function load() {
  loading.value = true
  try {
    const data = await pageUsers(query)
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
  Object.assign(query, { page: 1, name: '', phone: '', campus: '' })
  load()
}

onMounted(load)
</script>
