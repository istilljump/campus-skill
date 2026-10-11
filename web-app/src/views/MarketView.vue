<template>
  <div class="page-card">
    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="服务市场" name="market" />
      <el-tab-pane label="我的预约" name="bookings" />
    </el-tabs>

    <!-- 服务市场 -->
    <template v-if="tab === 'market'">
      <div class="filter-bar">
        <el-input v-model="mq.keyword" placeholder="搜索技能服务，回车搜索" clearable style="width: 240px" @keyup.enter="onSearch" />
        <el-select v-model="mq.categoryId" placeholder="技能类目" clearable style="width: 140px" @change="onSearch">
          <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <el-select v-model="mq.sort" style="width: 130px" @change="onSearch">
          <el-option label="综合推荐" value="default" />
          <el-option label="价格最低" value="price" />
          <el-option label="评分优先" value="score" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
      </div>

      <div class="svc-grid" v-loading="loading">
        <div v-for="s in market" :key="s.id" class="svc-card" @click="openSvc(s)">
          <div class="hall-head">
            <span class="title">{{ s.title }}</span>
            <span class="money">{{ fmtMoney(s.price) }}</span>
          </div>
          <div class="muted meta">{{ s.deliveryDays }}天交付 · {{ modeText(s.serviceMode) }}<template v-if="s.salesCount"> · 已售 {{ s.salesCount }}</template></div>
          <div class="hall-foot">
            <span class="muted">{{ s.skillerName }}</span>
            <span class="muted">评分 {{ s.skillerScore ?? '-' }} · 信用 {{ s.skillerCreditScore ?? '-' }}</span>
          </div>
        </div>
        <el-empty v-if="!loading && !market.length" description="市场暂无服务，稍后再来看看" style="grid-column: 1/-1" />
      </div>
      <div class="table-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          v-model:current-page="mq.page" :page-size="mq.pageSize" @change="load" />
      </div>
    </template>

    <!-- 我的预约 -->
    <template v-else>
      <el-table :data="bookings" v-loading="loading" stripe>
        <el-table-column label="服务" min-width="180">
          <template #default="{ row }">{{ row.serviceTitle || row.serviceName }}</template>
        </el-table-column>
        <el-table-column prop="expectTime" label="期望时间" width="170" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="BOOKING_STATUS[row.status]?.type">{{ BOOKING_STATUS[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" plain type="danger" @click="onCancelBooking(row)">取消预约</el-button>
            <el-button v-if="row.status === 1" size="small" type="primary" @click="onPayBooking(row)">去支付</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无预约记录" :image-size="72" /></template>
      </el-table>
    </template>

    <!-- 服务详情 / 预约弹窗 -->
    <el-dialog v-model="svcVisible" :title="svcDetail?.title" width="520px">
      <template v-if="svcDetail">
        <div class="order-line">
          <span class="money" style="font-size:20px">{{ fmtMoney(svcDetail.price) }}</span>
          <span class="muted">{{ svcDetail.deliveryDays }}天交付 · {{ modeText(svcDetail.serviceMode) }}</span>
        </div>
        <p class="desc">{{ svcDetail.description || '（服务详情待补充）' }}</p>
        <div class="muted" style="font-size:13px">技能者：{{ svcDetail.skillerName }} · 评分 {{ svcDetail.skillerScore ?? '-' }} · 信用 {{ svcDetail.skillerCreditScore ?? '-' }}</div>
        <el-divider />
        <el-form label-width="80px">
          <el-form-item label="期望时间">
            <el-input v-model="bookingForm.expectTime" placeholder="格式 2026-10-20 14:00" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="bookingForm.remark" type="textarea" :rows="2" placeholder="补充说明（选填）" />
          </el-form-item>
        </el-form>
        <el-button type="primary" style="width:100%" :loading="busy" @click="onSubmitBooking">提交预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { listCategories, listMarket, pageBookings, createBooking, cancelBooking, payBooking } from '@/api'
import { BOOKING_STATUS, fmtMoney, modeText } from '@/utils/dict'

const tab = ref('market')
const types = ref([])
const loading = ref(false)
const busy = ref(false)

// 服务市场
const mq = reactive({ page: 1, pageSize: 12, keyword: '', categoryId: null, sort: 'default' })
const market = ref([])
const total = ref(0)

// 预约
const bookings = ref([])
const svcVisible = ref(false)
const svcDetail = ref(null)
const bookingForm = reactive({ serviceItemId: null, expectTime: '', remark: '' })

async function load() {
  loading.value = true
  try {
    if (tab.value === 'market') {
      const data = await listMarket({ ...mq })
      market.value = data.records || []
      total.value = Number(data.total) || 0
    } else {
      const data = await pageBookings({ page: 1, pageSize: 20 })
      bookings.value = data.records || []
    }
  } finally {
    loading.value = false
  }
}

function onSearch() {
  mq.page = 1
  load()
}

function openSvc(s) {
  svcDetail.value = s
  bookingForm.serviceItemId = s.id
  bookingForm.expectTime = ''
  bookingForm.remark = ''
  svcVisible.value = true
}

async function onSubmitBooking() {
  if (!bookingForm.expectTime) return ElMessage.warning('请填写期望服务时间，如 2026-10-20 14:00')
  busy.value = true
  try {
    await createBooking({ ...bookingForm })
    ElMessage.success('预约成功，等待技能者确认')
    svcVisible.value = false
    tab.value = 'bookings'
    load()
  } finally {
    busy.value = false
  }
}

async function onCancelBooking(row) {
  await ElMessageBox.confirm('确定取消该预约吗？', '取消预约', { type: 'warning' })
  await cancelBooking(row.id)
  ElMessage.success('预约已取消')
  load()
}

// 与 H5 一致：预约支付 = 预约下单 + 订单支付 两步
async function onPayBooking(row) {
  busy.value = true
  try {
    const pr = await payBooking(row.id)
    await payOrderSafe(pr?.orderNumber)
  } finally {
    busy.value = false
  }
}

async function payOrderSafe(orderNumber) {
  try {
    const { payOrder } = await import('@/api')
    await payOrder(orderNumber)
    ElMessage.success('已下单并支付成功')
    load()
  } catch (e) { /* 余额不足等错误由拦截器统一提示 */ }
}

onMounted(async () => {
  load()
  types.value = await listCategories() || []
})
</script>

<style scoped>
.svc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
  min-height: 120px;
}

.svc-card {
  border: 1px solid rgba(255, 255, 255, .1);
  background: linear-gradient(160deg, rgba(255,255,255,.07), rgba(255,255,255,.04) 55%);
  border-radius: 14px;
  padding: 16px;
  cursor: pointer;
  transition: transform .16s ease, border-color .16s ease;
}

.svc-card:hover { transform: translateY(-2px); border-color: rgba(255, 255, 255, .22); }
.hall-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.title { font-weight: 600; color: #e6edf7; }
.meta { margin: 8px 0; font-size: 13px; }
.hall-foot { display: flex; justify-content: space-between; font-size: 12px; }
.desc { white-space: pre-wrap; color: #c3cede; font-size: 14px; margin: 10px 0; }
</style>
