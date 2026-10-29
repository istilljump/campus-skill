<template>
  <div>
    <!-- 批次汇总 -->
    <div class="page-card">
      <div class="reconcile-head">
        <div class="card-title"><el-icon><Coin /></el-icon><span>资金对账</span></div>
        <el-button type="primary" :loading="running" @click="onRun">立即对账</el-button>
      </div>
      <p class="muted" style="font-size:12px;margin:4px 0 12px">
        每小时自动执行一次全量对账：核对「钱包余额 = 按流水重算的期望余额」恒等式，差异行落库告警。
        支付/提现另有幂等令牌防护，对账是资金安全的最后防线。
      </p>
      <el-row :gutter="16" v-loading="summaryLoading">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">最近批次</div>
            <div class="stat-value small">{{ summary.batchNo || '尚未执行' }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">检查钱包数</div>
            <div class="stat-value">{{ summary.checkedWallets ?? '—' }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">差异钱包数</div>
            <div class="stat-value" :style="{ color: (summary.mismatchCount || 0) > 0 ? '#f56c6c' : '#00b578' }">
              {{ summary.mismatchCount ?? '—' }}
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">对账结论</div>
            <div class="stat-value">
              <el-tag v-if="summary.allMatched === true" type="success" effect="dark">账实相符</el-tag>
              <el-tag v-else-if="summary.allMatched === false" type="danger" effect="dark">存在差异</el-tag>
              <span v-else class="muted">—</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 差异明细 -->
    <div class="page-card" style="margin-top:16px">
      <div class="reconcile-head">
        <div class="card-title"><el-icon><Warning /></el-icon><span>差异明细</span></div>
        <div style="display:flex;gap:8px">
          <el-input v-model="query.batchNo" placeholder="批次号（留空查最近）" clearable style="width:220px" />
          <el-button :icon="Search" @click="load">查询</el-button>
          <el-button :disabled="!summary.batchNo || !hasRecords" :loading="exporting" @click="onExport">
            导出对账报告
          </el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="batchNo" label="批次号" width="180" />
        <el-table-column prop="walletId" label="钱包ID" width="90" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column label="当前余额" width="120">
          <template #default="{ row }"><span class="money">{{ fmtMoney(row.balance) }}</span></template>
        </el-table-column>
        <el-table-column label="期望余额" width="120">
          <template #default="{ row }"><span class="money">{{ fmtMoney(row.expectedBalance) }}</span></template>
        </el-table-column>
        <el-table-column label="差异金额" width="130">
          <template #default="{ row }">
            <span class="money" :style="{ color: Number(row.diffAmount) > 0 ? '#e6a23c' : '#f56c6c' }">
              {{ fmtMoney(row.diffAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="txCount" label="流水笔数" width="100" />
        <el-table-column label="对账时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <template #empty><el-empty description="无差异记录——账实相符" :image-size="72" /></template>
      </el-table>

      <div class="table-pager">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
          v-model:current-page="query.page" v-model:page-size="query.pageSize" :page-sizes="[10, 20, 50]"
          @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Coin, Search, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { runReconcile, reconcileSummary, pageReconcileRecords, exportReconcile } from '@/api'
import { fmtTime, fmtMoney } from '@/utils/dict'

const summary = ref({})
const summaryLoading = ref(false)
const running = ref(false)
const list = ref([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const query = reactive({ page: 1, pageSize: 10, batchNo: '' })

const hasRecords = computed(() => total.value > 0)

async function loadSummary() {
  summaryLoading.value = true
  try {
    summary.value = await reconcileSummary() || {}
  } finally {
    summaryLoading.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.batchNo) params.batchNo = query.batchNo
    const data = await pageReconcileRecords(params)
    list.value = data?.records || []
    total.value = Number(data?.total) || 0
  } finally {
    loading.value = false
  }
}

async function onRun() {
  running.value = true
  try {
    const s = await runReconcile()
    summary.value = s || {}
    ElMessage.success(s?.allMatched ? `对账完成：${s.checkedWallets} 个钱包全部一致` : `对账完成：发现 ${s?.mismatchCount} 个差异钱包`)
    query.batchNo = ''
    query.page = 1
    await load()
  } finally {
    running.value = false
  }
}

async function onExport() {
  exporting.value = true
  try {
    const blob = await exportReconcile(summary.value.batchNo)
    const url = URL.createObjectURL(new Blob([blob], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
    const a = document.createElement('a')
    a.href = url
    a.download = `对账报告-${summary.value.batchNo}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadSummary()
  load()
})
</script>

<style scoped>
.reconcile-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-card {
  background: #f7f9fc;
  border-radius: 10px;
  padding: 14px 16px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  margin-top: 4px;
}

.stat-value.small {
  font-size: 16px;
  font-weight: 600;
  line-height: 30px;
}
</style>
