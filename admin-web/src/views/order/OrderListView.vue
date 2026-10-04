<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="订单状态" clearable style="width: 140px">
        <el-option v-for="(v, k) in ORDER_STATUS" :key="k" :label="v.label" :value="Number(k)" />
      </el-select>
      <el-select v-model="query.typeId" placeholder="技能类目" clearable style="width: 140px">
        <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
      <el-input v-model="query.campus" placeholder="校区" clearable />
      <el-input v-model="query.keyword" placeholder="订单号 / 标题" clearable style="width: 220px" @keyup.enter="onSearch" />
      <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="number" label="订单号" width="200" />
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
      <el-table-column prop="typeName" label="类型" width="100" />
      <el-table-column prop="campus" label="校区" width="110" />
      <el-table-column label="悬赏" width="90">
        <template #default="{ row }"><span class="money">{{ fmtMoney(row.rewardAmount) }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="ORDER_STATUS[row.status]?.type">{{ ORDER_STATUS[row.status]?.label || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申诉" width="70">
        <template #default="{ row }">
          <el-tag v-if="row.isAppealed === 1" type="danger" size="small">申诉中</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publisherName" label="发单用户" width="100" />
      <el-table-column prop="runnerName" label="技能者" width="100">
        <template #default="{ row }">{{ row.runnerName || '-' }}</template>
      </el-table-column>
      <el-table-column label="下单时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pager">
      <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.pageSize" :page-sizes="[10, 20, 50]"
        @change="load" />
    </div>

    <el-drawer v-model="detailVisible" title="订单详情" size="480px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">{{ detail.number }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="需求描述">{{ detail.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.typeName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="ORDER_STATUS[detail.status]?.type">{{ ORDER_STATUS[detail.status]?.label }}</el-tag>
            <el-tag v-if="detail.isAppealed === 1" type="danger" style="margin-left: 8px">申诉中</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="取件地址">{{ detail.pickupAddress }}</el-descriptions-item>
          <el-descriptions-item label="送达地址">{{ detail.deliveryAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="校区">{{ detail.campus }}</el-descriptions-item>
          <el-descriptions-item label="悬赏金额">{{ fmtMoney(detail.rewardAmount) }}</el-descriptions-item>
          <el-descriptions-item label="平台服务费">{{ fmtMoney(detail.platformFee) }}</el-descriptions-item>
          <el-descriptions-item label="技能者实得">{{ fmtMoney(detail.runnerIncome) }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ detail.payStatus === 1 ? '已支付' : '未支付' }}</el-descriptions-item>
          <el-descriptions-item label="发单用户">
            {{ detail.publisherName || '-' }} {{ detail.publisherPhone || '' }}
          </el-descriptions-item>
          <el-descriptions-item label="技能者">
            {{ detail.runnerName || '-' }} {{ detail.runnerPhone || '' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.cancelReason" label="取消原因">
            {{ detail.cancelReason }}（{{ CANCEL_BY[detail.cancelBy] || '-' }}）
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.runnerScore" label="技能者评分">{{ detail.runnerScore }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="timeline-title">订单时间线</h4>
        <el-timeline>
          <el-timeline-item v-for="node in timeline" :key="node.label" :timestamp="node.time" :type="node.time ? 'primary' : 'info'">
            {{ node.label }}
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { pageOrders, listTypes, getOrderDetail } from '@/api'
import { ORDER_STATUS, CANCEL_BY, fmtTime, fmtMoney } from '@/utils/dict'

const query = reactive({ page: 1, pageSize: 10, status: null, typeId: null, campus: '', keyword: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const types = ref([])

const detailVisible = ref(false)
const detail = ref(null)

const timeline = computed(() => [
  { label: '下单', time: fmtTime(detail.value?.orderTime) },
  { label: '支付', time: fmtTime(detail.value?.payTime) },
  { label: '完成', time: fmtTime(detail.value?.finishTime) },
])

async function load() {
  loading.value = true
  try {
    const data = await pageOrders(query)
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
  Object.assign(query, { page: 1, status: null, typeId: null, campus: '', keyword: '' })
  load()
}

async function openDetail(id) {
  detail.value = await getOrderDetail(id)
  detailVisible.value = true
}

onMounted(async () => {
  load()
  types.value = await listTypes()
})
</script>

<style scoped>
.timeline-title {
  margin: 20px 0 12px;
}
</style>
