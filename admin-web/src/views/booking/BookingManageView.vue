<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="预约状态" clearable style="width: 140px">
        <el-option v-for="(v, k) in BOOKING_STATUS" :key="k" :label="v.label" :value="Number(k)" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
      <span class="muted tip-hint">仅「待确认」的预约可由管理员取消，取消后双端收到站内通知</span>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="serviceTitle" label="服务" min-width="180" show-overflow-tooltip />
      <el-table-column label="服务价格" width="100">
        <template #default="{ row }"><span class="money">{{ fmtMoney(row.servicePrice) }}</span></template>
      </el-table-column>
      <el-table-column prop="userName" label="预约用户" width="110">
        <template #default="{ row }">{{ row.userName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="skillerName" label="技能者" width="110">
        <template #default="{ row }">{{ row.skillerName || '-' }}</template>
      </el-table-column>
      <el-table-column label="期望时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.expectTime) }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="需求备注" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="BOOKING_STATUS[row.status]?.type">{{ BOOKING_STATUS[row.status]?.label || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" link type="danger" @click="onCancel(row)">取消预约</el-button>
          <span v-else class="muted">-</span>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { pageBookings, cancelBooking } from '@/api'
import { BOOKING_STATUS, fmtTime, fmtMoney } from '@/utils/dict'

const query = reactive({ page: 1, pageSize: 10, status: null })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await pageBookings(query)
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
  Object.assign(query, { page: 1, status: null })
  load()
}

async function onCancel(row) {
  await ElMessageBox.confirm(
    `确定取消该预约吗？取消后用户与技能者都会收到站内通知。`,
    '取消预约',
    { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '返回' },
  )
  await cancelBooking(row.id)
  ElMessage.success('预约已取消')
  load()
}

onMounted(load)
</script>

<style scoped>
.tip-hint {
  margin-left: auto;
}
</style>
