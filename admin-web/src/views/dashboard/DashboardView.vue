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
        <el-button size="small" text :icon="Refresh" style="color: #00aeec" @click="loadAll">刷新</el-button>
        <el-button size="small" text :icon="FullScreen" style="color: #00aeec" @click="toggleFullscreen">全屏</el-button>
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

    <!-- 资金与信任（v11 可信交易保障体系上屏） -->
    <div class="trust-row">
      <div class="trust-caption">
        <span class="trust-caption-bar"></span>资金与信任
      </div>
      <div class="trust-grid">
        <div v-for="t in trustCards" :key="t.label" class="trust-card" :class="{ clickable: t.to }"
             @click="t.to && router.push(t.to)">
          <div class="metric-icon" :style="{ background: t.color }">
            <el-icon :size="20"><component :is="t.icon" /></el-icon>
          </div>
          <div class="trust-body">
            <div class="metric-value" :style="t.alert ? { color: '#f25d5d' } : null">
              {{ t.value }}
            </div>
            <div class="metric-label">{{ t.label }}</div>
            <div class="metric-sub">{{ t.sub }}</div>
          </div>
          <el-icon v-if="t.to" class="trust-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="chart-panel span-full">
        <div class="panel-title">订单量 / 交易额趋势</div>
        <div ref="trendRef" class="chart"></div>
      </div>
      <div class="chart-panel span-3">
        <div class="panel-title">交易转化漏斗（发布 → 支付 → 接单 → 交付 → 验收）</div>
        <div ref="funnelRef" class="chart"></div>
      </div>
      <div class="chart-panel span-3">
        <div class="panel-title">订单发布时段热度（近7天 · 0-23时）</div>
        <div ref="hourlyRef" class="chart"></div>
      </div>
      <div class="chart-panel span-2">
        <div class="panel-title">热门技能类目 TOP5</div>
        <div ref="typeRef" class="chart chart-tall"></div>
      </div>
      <div class="chart-panel span-2">
        <div class="panel-title">各校区订单占比</div>
        <div ref="campusRef" class="chart chart-tall"></div>
      </div>
      <div class="chart-panel span-2">
        <div class="panel-title">订单状态分布</div>
        <div ref="statusRef" class="chart chart-tall"></div>
      </div>
      <div class="chart-panel span-full">
        <div class="panel-title">类目供需缺口对比（在售服务 = 供给 · 近30天发单 = 需求，按缺口排序）</div>
        <div ref="supplyRef" class="chart chart-tall"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Refresh, FullScreen } from '@element-plus/icons-vue'
import {
  overviewStats, trendStats, typeRankStats, campusRankStats, supplyDemandStats,
  trustStats, funnelStats, hourlyStats,
} from '@/api'

const router = useRouter()
const trendDays = ref(7)
const dashRef = ref(null)
const trendRef = ref(null)
const typeRef = ref(null)
const campusRef = ref(null)
const statusRef = ref(null)
const supplyRef = ref(null)
const funnelRef = ref(null)
const hourlyRef = ref(null)

const overview = ref({})
const trend = ref([])
const typeRank = ref([])
const campusRank = ref([])
const supplyDemand = ref([])
const trust = ref({})
const funnel = ref([])
const hourly = ref([])

let charts = []
let timer = null

const metrics = computed(() => [
  { label: '累计订单量', value: overview.value.totalOrders ?? '-', icon: 'Tickets', color: '#00aeec' },
  { label: '今日订单', value: overview.value.todayOrders ?? '-', icon: 'AlarmClock', color: '#06b6d4' },
  { label: '待接单', value: overview.value.pendingCount ?? '-', icon: 'Bell', color: '#ffb027' },
  { label: '进行中', value: overview.value.inProgressCount ?? '-', icon: 'Van', color: '#fb7299' },
  { label: '已完成', value: overview.value.completedCount ?? '-', icon: 'CircleCheckFilled', color: '#2ac864' },
  { label: '已取消', value: overview.value.cancelledCount ?? '-', icon: 'CircleCloseFilled', color: '#f25d5d' },
  { label: '累计交易额', value: `¥${Number(overview.value.totalReward ?? 0).toFixed(2)}`, icon: 'Coin', color: '#d97706' },
  { label: '平台服务费', value: `¥${Number(overview.value.totalPlatformFee ?? 0).toFixed(2)}`, icon: 'Wallet', color: '#fb7299' },
])

const money = (v) => `¥${Number(v ?? 0).toFixed(2)}`

const trustCards = computed(() => {
  const t = trust.value
  const mismatch = Number(t.reconcileMismatch ?? 0)
  return [
    { label: '托管中金额', value: money(t.escrowAmount), sub: '担保交易 · 已支付未结算', icon: 'Lock', color: '#d97706' },
    { label: '钱包总余额', value: money(t.walletBalance), sub: '全部钱包账户可用余额', icon: 'Wallet', color: '#00aeec' },
    { label: '提现在途', value: money(t.withdrawPending), sub: '待人工审核打款', icon: 'Money', color: '#06b6d4' },
    {
      label: '风控未处置', value: t.riskUnhandled ?? '-', alert: (t.riskUnhandled ?? 0) > 0,
      sub: `近7天告警 ${t.risk7d ?? 0} 条`, icon: 'WarningFilled',
      color: (t.riskUnhandled ?? 0) > 0 ? '#f25d5d' : '#9499a0', to: '/risk',
    },
    {
      label: '资金对账', value: t.reconcileAllMatched === false ? `差异 ${mismatch} 笔` : '账实相符',
      alert: t.reconcileAllMatched === false,
      sub: t.reconcileBatchNo ? `${t.reconcileBatchNo} · 检查 ${t.reconcileWallets ?? 0} 钱包` : '尚未执行对账',
      icon: t.reconcileAllMatched === false ? 'CircleCloseFilled' : 'CircleCheckFilled',
      color: t.reconcileAllMatched === false ? '#f25d5d' : '#2ac864', to: '/reconcile',
    },
  ]
})

const DARK_TEXT = '#61666d'

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
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#c9cdd4' } }, axisLabel: { color: DARK_TEXT } },
    yAxis: [
      { type: 'value', name: '单量', splitLine: { lineStyle: { color: '#e3e5e7' } }, axisLabel: { color: DARK_TEXT }, nameTextStyle: { color: DARK_TEXT } },
      { type: 'value', name: '金额(元)', splitLine: { show: false }, axisLabel: { color: DARK_TEXT }, nameTextStyle: { color: DARK_TEXT } },
    ],
    series: [
      { name: '新增订单', type: 'line', smooth: true, data: trend.value.map((t) => t.orderCount), itemStyle: { color: '#00aeec' }, areaStyle: { color: 'rgba(0,174,236,0.15)' } },
      { name: '完成订单', type: 'line', smooth: true, data: trend.value.map((t) => t.completedCount), itemStyle: { color: '#2ac864' } },
      { name: '交易额', type: 'line', smooth: true, yAxisIndex: 1, data: trend.value.map((t) => Number(t.amount)), itemStyle: { color: '#ffb027' }, lineStyle: { type: 'dashed' } },
    ],
  })
}

function renderTypeRank() {
  const data = [...typeRank.value].reverse()
  initOrResize(typeRef.value, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 90, right: 30, top: 16, bottom: 28 },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#e3e5e7' } }, axisLabel: { color: DARK_TEXT } },
    yAxis: { type: 'category', data: data.map((d) => d.name), axisLabel: { color: DARK_TEXT, width: 80, overflow: 'truncate' } },
    series: [{
      type: 'bar', barWidth: 16, data: data.map((d) => d.count),
      itemStyle: { borderRadius: [0, 8, 8, 0], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#00aeec' }, { offset: 1, color: '#4dc3f2' }]) },
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
        itemStyle: { color: ['#00aeec', '#2ac864', '#fb7299', '#ffb027', '#4dc3f2', '#9499a0', '#f25d5d'][i % 7] },
      })),
      label: { color: DARK_TEXT, formatter: '{d}%' },
      itemStyle: { borderColor: '#ffffff', borderWidth: 2 },
    }],
  })
}

function renderStatus() {
  const o = overview.value
  const known = (o.pendingCount ?? 0) + (o.inProgressCount ?? 0) + (o.completedCount ?? 0) + (o.cancelledCount ?? 0)
  const other = Math.max((o.totalOrders ?? 0) - known, 0)
  const data = [
    { name: '待接单', value: o.pendingCount ?? 0, itemStyle: { color: '#ffb027' } },
    { name: '进行中', value: o.inProgressCount ?? 0, itemStyle: { color: '#00aeec' } },
    { name: '已完成', value: o.completedCount ?? 0, itemStyle: { color: '#2ac864' } },
    { name: '已取消', value: o.cancelledCount ?? 0, itemStyle: { color: '#f25d5d' } },
  ]
  if (other > 0) data.push({ name: '其他', value: other, itemStyle: { color: '#9499a0' } })
  initOrResize(statusRef.value, {
    animation: false,
    tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: DARK_TEXT }, type: 'scroll' },
    series: [{
      type: 'pie', radius: '55%', center: ['50%', '42%'],
      data: data.filter((d) => d.value > 0),
      label: { color: DARK_TEXT, formatter: '{d}%' },
      itemStyle: { borderColor: '#ffffff', borderWidth: 2 },
    }],
  })
}

function renderFunnel() {
  const colors = ['#00aeec', '#4dc3f2', '#2ac864', '#ffb027', '#fb7299']
  initOrResize(funnelRef.value, {
    tooltip: {
      trigger: 'item',
      formatter: (p) => {
        const i = p.dataIndex
        const cur = funnel.value[i]
        const prev = funnel.value[i - 1]
        const conv = prev && prev.value > 0 ? `（较上环节 ${((cur.value / prev.value) * 100).toFixed(1)}%）` : ''
        return `${cur.name}：<b>${cur.value}</b> 单 ${conv}`
      },
    },
    series: [{
      type: 'funnel', left: '8%', width: '84%', top: 8, bottom: 8,
      sort: 'descending', gap: 4, minSize: '10%',
      label: { show: true, position: 'inside', color: '#fff', formatter: (p) => `${p.name}  ${p.value}` },
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      data: funnel.value.map((s, i) => ({ name: s.name, value: s.value, itemStyle: { color: colors[i % colors.length] } })),
    }],
  })
}

function renderHourly() {
  const max = Math.max(...hourly.value.map((h) => h.value ?? h.count ?? 0), 0)
  initOrResize(hourlyRef.value, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: (ps) => `${ps[0].name}：<b>${ps[0].value}</b> 单发布` },
    grid: { left: 40, right: 20, top: 24, bottom: 26 },
    xAxis: { type: 'category', data: hourly.value.map((h) => h.name), axisLine: { lineStyle: { color: '#c9cdd4' } }, axisLabel: { color: DARK_TEXT, interval: 1 } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#e3e5e7' } }, axisLabel: { color: DARK_TEXT } },
    series: [{
      type: 'bar', barWidth: '62%',
      data: hourly.value.map((h) => {
        const v = h.value ?? h.count ?? 0
        return {
          value: v,
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            // 峰值时段高亮粉色，其余时段主题蓝渐变
            color: v > 0 && v === max
              ? '#fb7299'
              : new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#4dc3f2' }, { offset: 1, color: 'rgba(0,174,236,0.25)' }]),
          },
        }
      }),
    }],
  })
}

function renderSupplyDemand() {
  const data = [...supplyDemand.value].reverse()
  initOrResize(supplyRef.value, {
    tooltip: {
      trigger: 'axis', axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const d = supplyDemand.value.find((x) => x.typeName === params[0].name)
        if (!d) return params[0].name
        const gap = d.demandCount - d.supplyCount
        return `${d.typeName}<br/>在售服务（供给）：<b>${d.supplyCount}</b><br/>近30天发单（需求）：<b>${d.demandCount}</b><br/>缺口：${gap > 0 ? `<b style="color:#f25d5d">需大于供 +${gap}</b>` : '供需平衡'}<br/>历史均价：¥${d.avgPrice}`
      },
    },
    legend: { data: ['在售服务（供给）', '近30天发单（需求）'], textStyle: { color: DARK_TEXT }, top: 0 },
    grid: { left: 90, right: 50, top: 30, bottom: 24 },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#e3e5e7' } }, axisLabel: { color: DARK_TEXT } },
    yAxis: { type: 'category', data: data.map((d) => d.typeName), axisLabel: { color: DARK_TEXT, width: 80, overflow: 'truncate' } },
    series: [
      {
        name: '在售服务（供给）', type: 'bar', barWidth: 11, data: data.map((d) => d.supplyCount),
        itemStyle: { borderRadius: [0, 6, 6, 0], color: '#4dc3f2' }, label: { show: true, position: 'right', color: DARK_TEXT },
      },
      {
        name: '近30天发单（需求）', type: 'bar', barWidth: 11,
        data: data.map((d) => ({
          value: d.demandCount,
          itemStyle: d.demandCount > d.supplyCount
            ? { borderRadius: [0, 6, 6, 0], color: '#fb7299' }
            : { borderRadius: [0, 6, 6, 0], color: '#c9cdd4' },
        })),
        label: { show: true, position: 'right', color: DARK_TEXT },
      },
    ],
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
    supplyDemandStats().then((d) => { supplyDemand.value = d || []; renderSupplyDemand() }),
    trustStats().then((d) => { trust.value = d || {} }),
    funnelStats().then((d) => { funnel.value = d || []; renderFunnel() }),
    hourlyStats(7).then((d) => { hourly.value = d || []; renderHourly() }),
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
  background: linear-gradient(160deg, #ffffff 0%, #f6f7f8 100%);
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
  color: #18191c;
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
  background: #4dc3f2;
  box-shadow: 0 0 12px rgba(0, 174, 236, 0.5);
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
  background: #f6f7f8;
  border: 1px solid #e3e5e7;
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
  color: #18191c;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
}

.metric-label {
  color: #9499a0;
  font-size: 12px;
  margin-top: 2px;
  white-space: nowrap;
}

/* ===== 资金与信任行（v2 新增） ===== */
.trust-row {
  margin-bottom: 12px;
}

.trust-caption {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #61666d;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.trust-caption-bar {
  width: 4px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, #00aeec, #fb7299);
}

.trust-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.trust-card {
  position: relative;
  background: #ffffff;
  border: 1px solid #e3e5e7;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: box-shadow 0.2s, transform 0.2s;
}

.trust-card.clickable {
  cursor: pointer;
}

.trust-card.clickable:hover {
  box-shadow: 0 4px 16px rgba(0, 174, 236, 0.15);
  transform: translateY(-2px);
}

.trust-body {
  min-width: 0;
}

.metric-sub {
  color: #c9cdd4;
  font-size: 11px;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.trust-arrow {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #c9cdd4;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.chart-panel {
  background: #ffffff;
  border: 1px solid #e3e5e7;
  border-radius: 10px;
  padding: 12px 14px;
}

.chart-panel.span-full {
  grid-column: span 6;
}

.chart-panel.span-2 {
  grid-column: span 2;
}

.chart-panel.span-3 {
  grid-column: span 3;
}

.panel-title {
  color: #61666d;
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
  .trust-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-panel.span-full, .chart-panel.span-2 {
  grid-column: span 2;
}

.chart-panel.span-3 { grid-column: span 2; }
}
</style>
