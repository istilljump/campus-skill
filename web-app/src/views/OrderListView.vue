<template>
  <div class="page-card">
    <el-tabs v-model="tab" @tab-change="onTab">
      <el-tab-pane v-for="(v, k) in tabs" :key="k" :label="v.label" :name="v.name" />
    </el-tabs>

    <el-table :data="list" v-loading="loading" stripe @row-click="(row) => $router.push(`/orders/${row.id}`)" style="cursor:pointer">
      <el-table-column prop="number" label="订单号" width="200" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="typeName" label="类型" width="100" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="ORDER_STATUS[row.status]?.type">{{ ORDER_STATUS[row.status]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="悬赏" width="100">
        <template #default="{ row }"><span class="money">{{ fmtMoney(row.rewardAmount) }}</span></template>
      </el-table-column>
      <el-table-column label="下单时间" width="160">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <template #empty>
        <el-empty :image-size="80" description="该状态下暂无订单">
          <el-button type="primary" @click="$router.push('/submit')">去发布悬赏</el-button>
        </el-empty>
      </template>
    </el-table>

    <div class="table-pager">
      <el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="page" :page-size="pageSize" @change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { pageOrders } from '@/api'
import { ORDER_STATUS, fmtMoney, fmtTime } from '@/utils/dict'

// 与 H5 端订单 Tab 一致：全部/待支付/待接单/进行中/待验收/已完成
const tabs = [
  { label: '全部', name: 'all', value: null },
  { label: '待支付', name: '1', value: 1 },
  { label: '待接单', name: '2', value: 2 },
  { label: '进行中', name: '3', value: 3 },
  { label: '待验收', name: '4', value: 4 },
  { label: '已完成', name: '5', value: 5 },
]
const tab = ref('all')
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize }
    const cur = tabs.find(t => t.name === tab.value)
    if (cur && cur.value) params.status = cur.value
    const data = await pageOrders(params)
    list.value = data.records || []
    total.value = Number(data.total) || 0
  } finally {
    loading.value = false
  }
}

function onTab() {
  page.value = 1
  load()
}

onMounted(load)
</script>
