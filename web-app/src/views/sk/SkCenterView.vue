<template>
  <div class="center" v-loading="loading">
    <!-- 主视觉：技能者数据总览 -->
    <div class="hero">
      <div class="hero-main">
        <h2>{{ center.name || '技能者' }}
          <el-tag effect="dark" type="warning" style="margin-left:6px">{{ levelName(center.runnerLevel) }}</el-tag>
          <el-tag v-if="center.skillLevel" effect="plain" style="margin-left:6px">作品定级 C{{ center.skillLevel }}</el-tag>
        </h2>
        <p>今日已接 {{ center.todayOrderCount ?? 0 }}/{{ center.dailyOrderLimit ?? 10 }} 单 · 评分 {{ center.score ?? '5.0' }} · 信用 {{ center.creditScore ?? 100 }}</p>
        <el-progress :percentage="limitPct" :stroke-width="8" color="#ffffff"
          :show-text="false" class="limit-bar" />
        <p class="muted-light">每日接单额度使用 {{ limitPct }}%</p>
      </div>
      <div class="stat-row">
        <div class="stat-box"><div class="stat-num">{{ fmtMoney(center.todayIncome) }}</div><div class="stat-lab">今日收入</div></div>
        <div class="stat-box"><div class="stat-num">{{ center.completedOrders ?? 0 }}</div><div class="stat-lab">完成单数</div></div>
        <div class="stat-box"><div class="stat-num">{{ fmtMoney(center.balance) }}</div><div class="stat-lab">钱包余额</div></div>
      </div>
    </div>

    <div class="center-grid">
      <!-- 当前任务 -->
      <div class="page-card" v-if="currentTask">
        <div class="card-title"><el-icon><Opportunity /></el-icon><span>🚀 当前任务</span>
          <el-tag type="primary" size="small">{{ ORDER_STATUS[currentTask.status]?.label }}</el-tag>
        </div>
        <div class="task-title">{{ currentTask.title }}</div>
        <div class="muted" style="font-size:13px">{{ currentTask.pickupAddress || '线上交付' }} → {{ currentTask.deliveryAddress || '线上交付' }}</div>
        <div class="card-foot">
          <span class="money">实得{{ fmtMoney(currentTask.runnerIncome) }}</span>
          <span>
            <el-button v-if="!currentTask.pickupTime" size="small" type="primary" plain @click="onPickup">开始服务</el-button>
            <el-button size="small" type="success" @click="$router.push('/sk/orders')">去交付</el-button>
          </span>
        </div>
      </div>

      <!-- 等级权益 -->
      <div class="page-card">
        <div class="card-title"><el-icon><TrophyBase /></el-icon><span>接单等级权益</span>
          <el-tag size="small" type="primary" style="margin-left:auto">{{ levelName(center.runnerLevel) }}</el-tag>
        </div>
        <el-progress :percentage="levelPct" :stroke-width="10" />
        <div class="muted" style="font-size:12px;margin-top:8px">
          累计完成 {{ center.completedOrders ?? 0 }} 单{{ nextGoal ? `，再完成 ${nextGoal} 单升级${nextLevelName}` : '，已达最高等级' }}
          · 当前每日可接 {{ center.dailyOrderLimit ?? 10 }} 单
        </div>
      </div>

      <!-- 近7日收入趋势（纯 CSS 柱状，零额外依赖） -->
      <div class="page-card">
        <div class="card-title"><el-icon><TrendCharts /></el-icon><span>近7日技能服务收入趋势</span></div>
        <div class="trend">
          <div v-for="p in trend" :key="p.date" class="trend-col">
            <div class="trend-val">{{ p.amount > 0 ? Number(p.amount).toFixed(0) : '' }}</div>
            <div class="trend-bar" :style="{ height: barH(p) + 'px' }"></div>
            <div class="trend-date">{{ String(p.date).slice(5) }}</div>
          </div>
        </div>
        <div class="muted" style="font-size:12px;margin-top:8px">近7日完成 {{ trendTotal }} 单 · 收入 {{ fmtMoney(trendIncome) }}</div>
      </div>

      <!-- 提现 -->
      <div class="page-card">
        <div class="card-title"><el-icon><Money /></el-icon><span>申请提现</span></div>
        <el-input-number v-model="wdAmount" :min="1" style="width:100%" placeholder="提现金额（元）" />
        <el-input v-model="wdPassword" type="password" show-password placeholder="提现密码（首次请先设置）" style="margin-top:10px" />
        <div style="display:flex;gap:10px;margin-top:12px">
          <el-button type="primary" style="flex:1" :loading="busy" @click="onWithdraw">申请提现</el-button>
          <el-button style="flex:1" :loading="busy" @click="onSetPwd">设置提现密码</el-button>
        </div>
        <el-divider />
        <div class="muted" style="margin-bottom:8px">提现记录</div>
        <el-table :data="wdRecords" size="small" max-height="200">
          <el-table-column label="金额" width="90">
            <template #default="{ row }">{{ fmtMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">{{ WD_STATUS[row.status] }}</template>
          </el-table-column>
          <el-table-column label="时间">
            <template #default="{ row }">{{ fmtTime(row.applyTime) }}</template>
          </el-table-column>
          <template #empty><el-empty :image-size="60" description="暂无提现记录" /></template>
        </el-table>
      </div>

      <!-- 收支明细 -->
      <div class="page-card">
        <div class="card-title"><el-icon><Wallet /></el-icon><span>收支明细</span></div>
        <el-table :data="txs" size="small" max-height="260" stripe>
          <el-table-column label="类型" width="110">
            <template #default="{ row }">{{ TX_TYPE[row.type] || '其他' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
          <el-table-column label="金额" width="100">
            <template #default="{ row }">
              <span :class="[1, 3, 5].includes(row.type) ? 'money' : 'neg'">
                {{ ([1, 3, 5].includes(row.type) ? '+' : '-') + fmtMoney(row.amount) }}
              </span>
            </template>
          </el-table-column>
          <template #empty><el-empty :image-size="60" description="暂无流水" /></template>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  skCenter, skTrend, skWallet, skPageTxs, skPageWithdraws,
  skApplyWithdraw, skSetWithdrawPwd, skPageOrders, skPickup,
} from '@/api'
import { TX_TYPE, WD_STATUS, LEVEL_NAMES, fmtMoney, fmtTime } from '@/utils/dict'
import { ORDER_STATUS } from '@/utils/dict'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const center = ref({})
const trend = ref([])
const currentTask = ref(null)
const wdRecords = ref([])
const txs = ref([])
const wdAmount = ref(null)
const wdPassword = ref('')
const busy = ref(false)
const loading = ref(false)

// 等级门槛：20/50/100 单晋升铜/银/金牌（与 H5 LEVEL_GOALS 一致）
const LEVEL_GOALS = { 1: 20, 2: 50, 3: 100 }
const levelName = (l) => LEVEL_NAMES[l] || '普通'
const levelPct = computed(() => {
  const done = center.value.completedOrders || 0
  const goal = LEVEL_GOALS[center.value.runnerLevel] || LEVEL_GOALS[3]
  return Math.min(100, Math.round(done / goal * 100))
})
const nextGoal = computed(() => {
  const done = center.value.completedOrders || 0
  const goal = LEVEL_GOALS[center.value.runnerLevel]
  return goal ? Math.max(goal - done, 0) : 0
})
const nextLevelName = computed(() => levelName((center.value.runnerLevel || 1) + 1))
const limitPct = computed(() => {
  const used = center.value.todayOrderCount || 0
  const lim = center.value.dailyOrderLimit || 10
  return Math.min(100, Math.round(used / lim * 100))
})

const trendTotal = computed(() => trend.value.reduce((s, t) => s + (t.orderCount || 0), 0))
const trendIncome = computed(() => trend.value.reduce((s, t) => s + Number(t.amount || 0), 0))
function barH(p) {
  const max = Math.max(...trend.value.map(t => Number(t.amount) || 0), 1)
  return Math.max(4, Math.round(Number(p.amount || 0) / max * 90))
}

async function load() {
  loading.value = true
  try {
    center.value = await skCenter() || {}
    store.center = center.value
    currentTask.value = ((await skPageOrders({ page: 1, pageSize: 5, status: 3 }))?.records || [])[0] || null
    trend.value = await skTrend(7) || []
    wdRecords.value = (await skPageWithdraws({ page: 1, pageSize: 10 }))?.records || []
    txs.value = (await skPageTxs({ page: 1, pageSize: 10 }))?.records || []
  } finally { loading.value = false }
}

async function onPickup() {
  await skPickup(currentTask.value.id)
  ElMessage.success('已确认开始服务')
  load()
}

async function onWithdraw() {
  busy.value = true
  try {
    await skApplyWithdraw({ amount: wdAmount.value, withdrawPassword: wdPassword.value })
    ElMessage.success('提现申请已提交')
    load()
  } finally { busy.value = false }
}

async function onSetPwd() {
  if (!wdPassword.value || wdPassword.value.length < 6) return ElMessage.warning('密码至少6位')
  await skSetWithdrawPwd(wdPassword.value)
  ElMessage.success('提现密码已设置')
}

onMounted(load)
</script>

<style scoped>
.hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  background: linear-gradient(135deg, rgba(0, 174, 236, .30), rgba(251, 114, 153, .22) 55%, rgba(14, 165, 233, .14));
  border: 1px solid rgba(0, 174, 236, .25);
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.hero-main h2 { margin: 0 0 8px; color: #18191c; }
.hero-main p { margin: 0 0 8px; color: rgba(24, 25, 28, .75); font-size: 14px; }
.limit-bar { max-width: 360px; }
.muted-light { color: rgba(24, 25, 28, .55); font-size: 12px; margin-top: 4px; }
.stat-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; align-self: center; }
.stat-box {
  min-width: 110px;
  background: rgba(255, 255, 255, .6);
  border: 1px solid rgba(255, 255, 255, .75);
  border-radius: 12px;
  padding: 14px;
  text-align: center;
}
.stat-num { font-weight: 700; font-size: 18px; color: #18191c; }
.stat-lab { font-size: 12px; color: rgba(24, 25, 28, .65); margin-top: 4px; }

.center-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 16px;
  align-items: start;
}

.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; margin-bottom: 12px; color: #18191c; }
.task-title { font-weight: 600; margin: 4px 0 6px; color: #18191c; }
.card-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; }

.trend { display: flex; align-items: flex-end; gap: 8px; height: 130px; }
.trend-col { flex: 1; text-align: center; display: flex; flex-direction: column; justify-content: flex-end; height: 100%; }
.trend-val { font-size: 11px; color: #61666d; min-height: 14px; }
.trend-bar {
  margin: 2px auto 0;
  width: 60%;
  min-height: 4px;
  border-radius: 4px 4px 0 0;
  background: linear-gradient(180deg, #00aeec, #fb7299);
}
.trend-date { font-size: 11px; color: #9499a0; margin-top: 6px; }
.neg { color: #f25d5d; font-weight: 600; }
</style>
