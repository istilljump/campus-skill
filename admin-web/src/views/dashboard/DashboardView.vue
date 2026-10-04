<template>
  <div class="dashboard" ref="dashRef">
    <div class="dash-header">
      <div class="dash-title">
        <span class="dash-dot"></span>
        CampusSkill 技能工坊平台 · 数据大屏
      </div>
      <div class="dash-actions">
        <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
          <el-radio-button :value="7">近7天</el-radio-button>
          <el-radio-button :value="30">近30天</el-radio-button>
        </el-radio-group>
        <el-button size="small" text :icon="Refresh" style="color: #93c5fd" @click="loadAll">刷新</el-button>
        <el-button size="small" text :icon="FullScreen" style="color: #93c5fd" @click="toggleFullscreen">全屏</el-button>
      </div>
    </div>

    <div class="metric-grid">
      <div v-for="m in metrics" :key="m.label" class="metric-card">
        <div class="metric-icon" :style="{ background: m.color }">
          <el-icon :size="22"><component :is="m.icon" /></el-icon>
        </div>
        <div>
          <div class="metric-value">{{ m.value }}</div>
          <div class="metric-label">{{ m.label }}</div>
        </div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="chart-panel span-full">
        <div class="panel-title">订单量 / 交易额趋势</div>
        <div ref="trendRef" class="chart"></div>
      </div>
      <div class="chart-panel">
        <div class="panel-title">热门技能类目 TOP5</div>
        <div ref="typeRef" class="chart chart-tall"></div>
      </div>
      <div class="chart-panel">
        <div class="panel-title">各校区订单占比</div>
        <div ref="campusRef" class="chart chart-tall"></div>
      </div>
      <div class="chart-panel">
        <div class="panel-title">订单状态分布</div>
        <div ref="statusRef" class="chart chart-tall"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { Refresh, FullScreen } from '@element-plus/icons-vue'
import { overviewStats, trendStats, typeRankStats, campusRankStats } from '@/api'

const trendDays = ref(7)
const dashRef = ref(null)
const trendRef = ref(null)
const typeRef = ref(null)
const campusRef = ref(null)
const statusRef = ref(null)

const overview = ref({})
const trend = ref([])
const typeRank = ref([])
const campusRank = ref([])

let charts = []
let timer = null

const metrics = computed(() => [
  { label: '累计订单量', value: overview.value.totalOrders ?? '-', icon: 'Tickets', color: '#3b82f6' },
  { label: '今日订单', value: overview.value.todayOrders ?? '-', icon: 'AlarmClock', color: '#06b6d4' },
  { label: '待接单', value: overview.value.pendingCount ?? '-', icon: 'Bell', color: '#f59e0b' },
  { label: '进行中', value: overview.value.inProgressCount ?? '-', icon: 'Van', color: '#8b5cf6' },
  { label: '已完成', value: overview.value.completedCount ?? '-', icon: 'CircleCheckFilled', color: '#22c55e' },
  { label: '已取消', value: overview.value.cancelledCount ?? '-', icon: 'CircleCloseFilled', color: '#ef4444' },
  { label: '累计交易额', value: `¥${Number(overview.value.totalReward ?? 0).toFixed(2)}`, icon: 'Coin', color: '#d97706' },
  { label: '平台服务费', value: `¥${Number(overview.value.totalPlatformFee ?? 0).toFixed(2)}`, icon: 'Wallet', color: '#ec4899' },
])

const DARK_TEXT = '#93a3bd'

function initChart(el) {
  const chart = echarts.init(el)
  charts.push(chart)
  return chart
}

function renderTrend() {
  const dates = trend.value.map((t) => t.date.slice(5))
  initOrResize(trendRef.value, {
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增订单', '完成订单', '交易额'], textStyle: { color: DARK_TEXT }, top: 0 },
    grid: { left: 50, right: 55, top: 36, bottom: 28 },
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#334766' } }, axisLabel: { color: DARK_TEXT } },
    yAxis: [
      { type: 'value', name: '单量', splitLine: { lineStyle: { color: '#1e2c47' } }, axisLabel: { color: DARK_TEXT }, nameTextStyle: { color: DARK_TEXT } },
      { type: 'value', name: '金额(元)', splitLine: { show: false }, axisLabel: { color: DARK_TEXT }, nameTextStyle: { color: DARK_TEXT } },
    ],
    series: [
      { name: '新增订单', type: 'line', smooth: true, data: trend.value.map((t) => t.orderCount), itemStyle: { color: '#3b82f6' }, areaStyle: { color: 'rgba(59,130,246,0.18)' } },
      { name: '完成订单', type: 'line', smooth: true, data: trend.value.map((t) => t.completedCount), itemStyle: { color: '#22c55e' } },
      { name: '交易额', type: 'line', smooth: true, yAxisIndex: 1, data: trend.value.map((t) => Number(t.amount)), itemStyle: { color: '#f59e0b' }, lineStyle: { type: 'dashed' } },
    ],
  })
}

function renderTypeRank() {
  const data = [...typeRank.value].reverse()
  initOrResize(typeRef.value, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 90, right: 30, top: 16, bottom: 28 },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#1e2c47' } }, axisLabel: { color: DARK_TEXT } },
    yAxis: { type: 'category', data: data.map((d) => d.name), axisLabel: { color: DARK_TEXT, width: 80, overflow: 'truncate' } },
    series: [{
      type: 'bar', barWidth: 16, data: data.map((d) => d.count),
      itemStyle: { borderRadius: [0, 8, 8, 0], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#3b82f6' }, { offset: 1, color: '#22d3ee' }]) },
      label: { show: true, position: 'right', color: DARK_TEXT },
    }],
  })
}

function renderCampus() {
  initOrResize(campusRef.value, {
    animation: false,
    tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: DARK_TEXT }, type: 'scroll' },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '44%'],
      data: campusRank.value.map((d, i) => ({
        name: d.name,
        value: d.count,
        itemStyle: { color: ['#3b82f6', '#22d3ee', '#8b5cf6', '#f59e0b', '#22c55e', '#ec4899', '#ef4444'][i % 7] },
      })),
      label: { color: DARK_TEXT, formatter: '{d}%' },
      itemStyle: { borderColor: '#0b1526', borderWidth: 2 },
    }],
  })
}

function renderStatus() {
  const o = overview.value
  const known = (o.pendingCount ?? 0) + (o.inProgressCount ?? 0) + (o.completedCount ?? 0) + (o.cancelledCount ?? 0)
  const other = Math.max((o.totalOrders ?? 0) - known, 0)
  const data = [
    { name: '待接单', value: o.pendingCount ?? 0, itemStyle: { color: '#f59e0b' } },
    { name: '进行中', value: o.inProgressCount ?? 0, itemStyle: { color: '#3b82f6' } },
    { name: '已完成', value: o.completedCount ?? 0, itemStyle: { color: '#22c55e' } },
    { name: '已取消', value: o.cancelledCount ?? 0, itemStyle: { color: '#ef4444' } },
  ]
  if (other > 0) data.push({ name: '其他', value: other, itemStyle: { color: '#64748b' } })
  initOrResize(statusRef.value, {
    animation: false,
    tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: DARK_TEXT }, type: 'scroll' },
    series: [{
      type: 'pie', radius: '55%', center: ['50%', '42%'],
      data: data.filter((d) => d.value > 0),
      label: { color: DARK_TEXT, formatter: '{d}%' },
      itemStyle: { borderColor: '#0b1526', borderWidth: 2 },
    }],
  })
}

const rendered = new WeakMap()

function initOrResize(el, option) {
  if (!el) return
  let chart = charts.find((c) => c.getDom() === el)
  if (!chart) {
    chart = initChart(el)
    rendered.set(el, true)
  }
  chart.setOption(option, true)
  chart.resize()
}

function onResize() {
  charts.forEach((c) => c.resize())
}

async function loadOverview() {
  overview.value = await overviewStats()
  renderStatus()
}

async function loadTrend() {
  trend.value = await trendStats(trendDays.value)
  renderTrend()
}

async function loadAll() {
  await Promise.all([
    loadOverview(),
    loadTrend(),
    typeRankStats(5).then((d) => { typeRank.value = d; renderTypeRank() }),
    campusRankStats().then((d) => { campusRank.value = d; renderCampus() }),
  ])
}

function toggleFullscreen() {
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    dashRef.value.requestFullscreen()
  }
}

onMounted(() => {
  loadAll()
  timer = setInterval(loadAll, 30000)
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  clearInterval(timer)
  window.removeEventListener('resize', onResize)
  charts.forEach((c) => c.dispose())
  charts = []
})
</script>

<style scoped>
.dashboard {
  min-height: calc(100vh - 92px);
  background: linear-gradient(160deg, #0b1526 0%, #101f38 100%);
  border-radius: 8px;
  padding: 16px 20px 20px;
}

.dash-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.dash-title {
  color: #e2e8f0;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: 2px;
}

.dash-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #22d3ee;
  box-shadow: 0 0 12px #22d3ee;
  animation: pulse 1.6s infinite;
}

@keyframes pulse {
  50% { opacity: 0.4; }
}

.dash-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.metric-card {
  background: rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 10px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.metric-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.metric-value {
  color: #f1f5f9;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
}

.metric-label {
  color: #7e8ca6;
  font-size: 12px;
  margin-top: 2px;
  white-space: nowrap;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.chart-panel {
  background: rgba(148, 163, 184, 0.06);
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 10px;
  padding: 12px 14px;
}

.chart-panel.span-full {
  grid-column: span 6;
}

.panel-title {
  color: #cbd5e1;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.chart {
  height: 260px;
}

.chart-tall {
  height: 280px;
}

@media (max-width: 1200px) {
  .metric-grid { grid-template-columns: repeat(4, 1fr); }
  .chart-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-panel.span-full { grid-column: span 2; }
}
</style>
