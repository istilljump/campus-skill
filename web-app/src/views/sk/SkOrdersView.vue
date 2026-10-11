<template>
  <div class="page-card">
    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="抢单大厅" name="hall" />
      <el-tab-pane label="我的接单" name="orders" />
      <el-tab-pane label="预约单" name="bookings" />
    </el-tabs>

    <!-- 抢单大厅 -->
    <template v-if="tab === 'hall'">
      <div class="filter-bar">
        <el-input v-model="hallKeyword" placeholder="搜索标题 / 订单号，回车搜索" clearable style="width: 240px" @keyup.enter="onHallSearch" />
        <el-select v-model="hallTypeId" placeholder="技能类目" clearable style="width: 140px" @change="onHallSearch">
          <el-option v-for="t in hallTypes" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="onHallSearch">查询</el-button>
        <span class="muted tip">悬赏金额为技能者实得（平台服务费已扣除）</span>
      </div>
      <div class="card-grid" v-loading="loading">
        <div v-for="o in hall" :key="o.id" class="mini-card">
          <div class="card-head">
            <span class="title">{{ o.title }}</span>
            <span class="money">实得{{ fmtMoney(o.runnerIncome) }}</span>
          </div>
          <div class="muted meta">{{ o.pickupAddress || '线上交付' }} → {{ o.deliveryAddress || '线上交付' }}</div>
          <div class="card-foot">
            <span class="muted">{{ o.campus }} · {{ o.publisherName }} 发布于 {{ relTime(o.createTime) }}</span>
            <el-button size="small" type="primary" :disabled="busy" @click="onGrab(o)">立即抢单</el-button>
          </div>
        </div>
        <el-empty v-if="!loading && !hall.length" description="悬赏大厅暂无需求" style="grid-column:1/-1" />
      </div>
    </template>

    <!-- 我的接单 -->
    <template v-else-if="tab === 'orders'">
      <el-table :data="myOrders" v-loading="loading" stripe>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="路线" min-width="180">
          <template #default="{ row }">{{ row.pickupAddress || '线上交付' }} → {{ row.deliveryAddress || '线上交付' }}</template>
        </el-table-column>
        <el-table-column label="实得" width="100">
          <template #default="{ row }"><span class="money">{{ fmtMoney(row.runnerIncome) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="ORDER_STATUS[row.status]?.type">{{ ORDER_STATUS[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.publisherPhone && row.status === 3" size="small" text :href="'tel:' + row.publisherPhone">📞联系用户</el-button>
            <el-button v-if="row.status === 3 && !row.pickupTime" size="small" type="primary" plain @click="onPickup(row)">开始服务</el-button>
            <el-button v-if="row.status === 3" size="small" type="success" @click="openDeliver(row)">交付成果</el-button>
            <el-button v-if="row.status === 8" size="small" type="warning" @click="onRework(row)">响应返修</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="还没有接单记录" :image-size="72" /></template>
      </el-table>
    </template>

    <!-- 预约单 -->
    <template v-else>
      <el-table :data="skBookings" v-loading="loading" stripe>
        <el-table-column label="服务" min-width="180">
          <template #default="{ row }">{{ row.serviceTitle || row.serviceName }}</template>
        </el-table-column>
        <el-table-column label="客户" width="110">
          <template #default="{ row }">{{ row.userName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="expectTime" label="期望时间" width="170" />
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="BOOKING_STATUS[row.status]?.type">{{ BOOKING_STATUS[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button size="small" type="primary" :disabled="busy" @click="onConfirmBooking(row)">接受预约</el-button>
              <el-button size="small" plain type="danger" :disabled="busy" @click="onRejectBooking(row)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无预约单" :image-size="72" /></template>
      </el-table>
    </template>

    <!-- 交付弹窗 -->
    <el-dialog v-model="deliverVisible" title="📦 交付成果" width="480px">
      <el-form label-position="top">
        <el-form-item label="交付物链接" required>
          <el-input v-model="deliverForm.deliverableUrl" placeholder="粘贴交付物链接（网盘/图片URL）" />
        </el-form-item>
        <el-form-item label="交付说明">
          <el-input v-model="deliverForm.deliverableNote" type="textarea" :rows="2" placeholder="补充说明（选填）" />
        </el-form-item>
      </el-form>
      <el-button type="primary" style="width:100%" :loading="busy" @click="onDeliver">确认交付</el-button>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import {
  skHall, skHallCategories, skGrab, skPageOrders, skPickup, skDeliver, skRespondRework,
  skPageBookings, skConfirmBooking, skRejectBooking,
} from '@/api'
import { ORDER_STATUS, BOOKING_STATUS, fmtMoney, relTime } from '@/utils/dict'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const tab = ref('hall')
const loading = ref(false)
const busy = ref(false)

// 大厅
const hall = ref([])
const hallTypes = ref([])
const hallKeyword = ref('')
const hallTypeId = ref(null)
const myOrders = ref([])
const skBookings = ref([])

// 交付
const deliverVisible = ref(false)
const deliverForm = reactive({ id: null, deliverableUrl: '', deliverableNote: '' })

async function load() {
  loading.value = true
  try {
    if (tab.value === 'hall') {
      const params = { page: 1, pageSize: 20 }
      if (hallKeyword.value) params.keyword = hallKeyword.value
      if (hallTypeId.value) params.typeId = hallTypeId.value
      const data = await skHall(params)
      hall.value = data.records || []
    } else if (tab.value === 'orders') {
      const data = await skPageOrders({ page: 1, pageSize: 20 })
      myOrders.value = data.records || []
    } else {
      const data = await skPageBookings({ page: 1, pageSize: 20 })
      skBookings.value = data.records || []
    }
  } finally {
    loading.value = false
  }
}

function onHallSearch() { load() }

async function onGrab(o) {
  busy.value = true
  try {
    await skGrab({ id: o.id })
    ElMessage.success('接单成功！请尽快开始服务')
    if (navigator.vibrate) navigator.vibrate([80, 40, 80])
    tab.value = 'orders'
    load()
  } finally { busy.value = false }
}

async function onPickup(row) {
  await skPickup(row.id)
  ElMessage.success('已确认开始服务')
  load()
}

function openDeliver(row) {
  deliverForm.id = row.id
  deliverForm.deliverableUrl = row.deliverableUrl || ''
  deliverForm.deliverableNote = row.deliverableNote || ''
  deliverVisible.value = true
}

async function onDeliver() {
  if (!deliverForm.deliverableUrl) return ElMessage.warning('请粘贴交付物链接')
  busy.value = true
  try {
    await skDeliver(deliverForm.id, { deliverableUrl: deliverForm.deliverableUrl, deliverableNote: deliverForm.deliverableNote })
    deliverVisible.value = false
    ElMessage.success('交付成功，等待用户验收')
    load()
  } finally { busy.value = false }
}

async function onRework(row) {
  await ElMessageBox.confirm('确认响应返修并重新开始服务吗？', '响应返修', { type: 'warning' })
  await skRespondRework(row.id)
  ElMessage.success('已响应返修，请尽快重新交付')
  load()
}

async function onConfirmBooking(row) {
  busy.value = true
  try {
    await skConfirmBooking(row.id)
    ElMessage.success('已接受预约，等待用户支付')
    load()
  } finally { busy.value = false }
}

async function onRejectBooking(row) {
  await ElMessageBox.confirm('确定拒绝该预约吗？', '拒绝预约', { type: 'warning' })
  await skRejectBooking(row.id)
  ElMessage.success('已拒绝预约')
  load()
}

onMounted(async () => {
  load()
  hallTypes.value = await skHallCategories() || []
})
</script>

<style scoped>
.tip { margin-left: auto; }
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
  min-height: 120px;
}
.mini-card {
  border: 1px solid rgba(255, 255, 255, .1);
  background: linear-gradient(160deg, rgba(255,255,255,.07), rgba(255,255,255,.04) 55%);
  border-radius: 14px;
  padding: 16px;
}
.card-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.title { font-weight: 600; color: #e6edf7; }
.meta { margin: 8px 0; font-size: 13px; }
.card-foot { display: flex; justify-content: space-between; align-items: center; font-size: 12px; }
</style>
