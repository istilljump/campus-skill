<template>
  <div class="wallet">
    <div class="hero">
      <p class="lab">钱包余额（元）</p>
      <h2 class="num">{{ fmtMoney(wallet.balance) }}</h2>
      <p class="sub">累计收入 {{ fmtMoney(wallet.totalIncome) }} · 支出 {{ fmtMoney(wallet.totalExpense) }}</p>
      <el-button type="primary" style="margin-top:14px" :loading="busy" @click="onRecharge">充值 50 元</el-button>
    </div>

    <div class="page-card">
      <div class="card-title"><el-icon><Wallet /></el-icon><span>收支明细</span></div>
      <el-table :data="txs" v-loading="loading" stripe>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">{{ TX_TYPE[row.type] || '其他' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span :class="[1, 3, 5].includes(row.type) ? 'money' : 'neg'">
              {{ ([1, 3, 5].includes(row.type) ? '+' : '-') + fmtMoney(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <template #empty><el-empty description="暂无流水" :image-size="72" /></template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getWallet, pageTxs, recharge } from '@/api'
import { TX_TYPE, fmtMoney, fmtTime } from '@/utils/dict'

const wallet = ref({})
const txs = ref([])
const loading = ref(false)
const busy = ref(false)

async function load() {
  loading.value = true
  try {
    wallet.value = await getWallet() || {}
    const data = await pageTxs({ page: 1, pageSize: 20 })
    txs.value = data.records || []
  } finally {
    loading.value = false
  }
}

async function onRecharge() {
  busy.value = true
  try {
    await recharge({ amount: 50 })
    ElMessage.success('充值成功')
    load()
  } finally { busy.value = false }
}

onMounted(load)
</script>

<style scoped>
.hero {
  text-align: center;
  background: linear-gradient(135deg, rgba(0, 174, 236, .28), rgba(251, 114, 153, .20) 55%, rgba(14, 165, 233, .14));
  border: 1px solid rgba(0, 174, 236, .25);
  border-radius: 16px;
  padding: 28px;
  margin-bottom: 16px;
}
.lab { margin: 0; color: rgba(24, 25, 28, .7); }
.num { margin: 8px 0; font-size: 40px; color: #00aeec; }
.sub { margin: 0; color: rgba(24, 25, 28, .6); font-size: 13px; }
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; margin-bottom: 12px; color: #18191c; }
.neg { color: #f25d5d; font-weight: 600; }
</style>
