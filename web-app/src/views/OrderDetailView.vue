<template>
  <div class="page-card detail-card" v-if="detail">
    <!-- 状态步骤条 -->
    <el-steps :active="stepActive" align-center class="steps">
      <el-step title="发单" /><el-step title="支付" /><el-step title="接单" />
      <el-step title="交付" /><el-step title="完成" />
    </el-steps>

    <div class="detail-head">
      <div>
        <h3 class="title">{{ detail.title }}
          <el-tag :type="ORDER_STATUS[detail.status]?.type">{{ ORDER_STATUS[detail.status]?.label }}</el-tag>
          <el-tag v-if="countdown" type="warning" effect="plain">⏱ {{ countdown }}</el-tag>
        </h3>
        <p class="desc">{{ detail.description || '（无需求描述）' }}</p>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-descriptions :column="1" border class="desc-box">
          <el-descriptions-item label="订单号">
            {{ detail.number }}
            <el-link type="primary" :underline="false" style="margin-left:8px" @click="copyText(detail.number)">复制</el-link>
          </el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.typeName }}</el-descriptions-item>
          <el-descriptions-item label="开始服务">{{ detail.pickupAddress || '线上交付' }}</el-descriptions-item>
          <el-descriptions-item label="交付地点">{{ detail.deliveryAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="悬赏金额">{{ fmtMoney(detail.rewardAmount) }}</el-descriptions-item>
          <el-descriptions-item label="平台服务费">{{ fmtMoney(detail.platformFee) }}</el-descriptions-item>
          <el-descriptions-item label="技能者">
            {{ detail.runnerName || '待接单' }}
            <el-link v-if="detail.runnerPhone" type="primary" :underline="false" style="margin-left:8px"
              :href="'tel:' + detail.runnerPhone">📞 {{ detail.runnerPhone }}</el-link>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 返修 / 仲裁提示 -->
        <el-alert v-if="detail.status === 8" type="warning" :closable="false" style="margin-top:12px"
          title="返修中：技能者正在重新交付，请耐心等待" />
        <el-alert v-if="detail.status === 9" type="error" :closable="false" style="margin-top:12px"
          title="仲裁中：平台将尽快裁定" />
      </el-col>

      <el-col :span="10">
        <!-- 交付成果 + 验收操作 -->
        <div class="page-card action-card" v-if="detail.status === 4">
          <div class="card-title"><el-icon><Box /></el-icon><span>交付成果</span></div>
          <div v-if="detail.deliverableUrl" class="muted deliver-line">
            交付物链接：<el-link type="primary" :href="detail.deliverableUrl" target="_blank">{{ detail.deliverableUrl }}</el-link>
            <el-link type="primary" :underline="false" style="margin-left:6px" @click="copyText(detail.deliverableUrl)">复制</el-link>
          </div>
          <div v-if="detail.deliverableNote" class="muted">交付说明：{{ detail.deliverableNote }}</div>
          <div class="muted" style="margin:10px 0">剩余返修次数：{{ 2 - (detail.reworkCount || 0) }} 次（最多 2 次）</div>
          <el-button type="primary" style="width:100%" :loading="busy" @click="onAccept">✅ 验收完成</el-button>
          <el-button style="width:100%;margin:10px 0 0" :loading="busy" @click="onRework">申请返修</el-button>
          <el-button style="width:100%;margin:10px 0 0" :loading="busy" @click="onDispute">发起仲裁</el-button>
        </div>

        <!-- 待支付 -->
        <div class="page-card action-card" v-if="detail.status === 1">
          <el-button type="primary" style="width:100%" :loading="busy" @click="onPay">立即支付</el-button>
          <el-button style="width:100%;margin:10px 0 0" :loading="busy" @click="onCancel">取消订单</el-button>
        </div>

        <!-- 追加悬赏 -->
        <div class="page-card action-card" v-if="detail.status === 2 && detail.payStatus === 1">
          <div class="card-title"><el-icon><Top /></el-icon><span>长时间无人接单？</span></div>
          <p class="muted">追加悬赏提高曝光，同时顺延 15 分钟接单超时</p>
          <el-input-number v-model="boostAmount" :min="1" :max="1000" style="width:100%" placeholder="追加金额（元）" />
          <el-button type="primary" plain style="width:100%;margin:10px 0 0" :loading="busy" @click="onBoost">追加悬赏</el-button>
          <el-button style="width:100%;margin:10px 0 0" :loading="busy" @click="onCancel">取消订单（全额退款）</el-button>
        </div>

        <!-- 评价 -->
        <div class="page-card action-card" v-if="detail.status === 5 && !reviewed">
          <div class="card-title"><el-icon><Star /></el-icon><span>评价本次服务</span></div>
          <el-rate v-model="reviewScore" style="margin-bottom:10px" />
          <el-input v-model="reviewContent" type="textarea" :rows="2" placeholder="说说这次体验（选填）" />
          <el-button type="primary" style="width:100%;margin:10px 0 0" :loading="busy" @click="onReview">提交评价</el-button>
        </div>
        <el-alert v-if="detail.status === 5 && reviewed" type="success" :closable="false" title="✅ 已完成并评价" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Box, Star, Top } from '@element-plus/icons-vue'
import {
  getOrderDetail, payOrder, boostOrder, cancelOrder, acceptOrder,
  reworkOrder, disputeOrder, getOrderReview, submitReview,
} from '@/api'
import { ORDER_STATUS, fmtMoney } from '@/utils/dict'

const route = useRoute()
const detail = ref(null)
const reviewed = ref(false)
const busy = ref(false)
const boostAmount = ref(null)
const reviewScore = ref(5)
const reviewContent = ref('')

// 状态 → 步骤条（与 H5 stepActive 映射一致）
const stepActive = computed(() => {
  const m = { 1: 0, 2: 1, 3: 2, 4: 3, 5: 4, 8: 3, 9: 3 }
  return m[detail.value?.status] ?? 0
})

// 关键节点倒计时（与 H5 countdown 逻辑一致）
const nowTick = ref(Date.now())
let timer = null
const parseTime = (s) => s ? new Date(String(s).replace(' ', 'T')).getTime() : 0
const countdown = computed(() => {
  const d = detail.value
  if (!d) return ''
  const now = nowTick.value
  if (d.status === 1 && d.orderTime) {
    const left = parseTime(d.orderTime) + 15 * 60 * 1000 - now
    if (left <= 0) return '即将超时关闭'
    return '支付剩余 ' + Math.floor(left / 60000) + ' 分 ' + Math.floor(left % 60000 / 1000) + ' 秒'
  }
  if (d.status === 4 && d.autoAcceptTime) {
    const left = parseTime(d.autoAcceptTime) - now
    if (left <= 0) return '即将自动验收'
    return Math.floor(left / 3600000) + ' 小时后自动验收'
  }
  if ((d.status === 3 || d.status === 8) && d.timeoutTime) {
    const left = parseTime(d.timeoutTime) - now
    if (left <= 0) return '已超时，待系统处理'
    return '交付剩余 ' + Math.floor(left / 3600000) + ' 时 ' + Math.floor(left % 3600000 / 60000) + ' 分'
  }
  return ''
})

async function load() {
  detail.value = await getOrderDetail(route.params.id)
  const rv = await getOrderReview(route.params.id)
  reviewed.value = !!(rv && rv.id)
}

async function onPay() {
  busy.value = true
  try {
    await payOrder(detail.value.number)
    ElMessage.success('支付成功')
    load()
  } finally { busy.value = false }
}

async function onBoost() {
  if (!boostAmount.value || boostAmount.value <= 0) return ElMessage.warning('请输入追加金额')
  busy.value = true
  try {
    await boostOrder({ id: detail.value.id, amount: boostAmount.value })
    ElMessage.success('悬赏追加成功')
    boostAmount.value = null
    load()
  } finally { busy.value = false }
}

async function onCancel() {
  await ElMessageBox.confirm('确定取消该订单吗？已支付金额将原路退回钱包', '取消订单', { type: 'warning' })
  await cancelOrder({ id: detail.value.id, cancelReason: '用户主动取消' })
  ElMessage.success('已取消并退款')
  load()
}

async function onAccept() {
  await ElMessageBox.confirm('确认验收该交付成果？确认后订单将完成并结算报酬', '验收完成', { type: 'warning' })
  await acceptOrder(detail.value.id)
  ElMessage.success('已验收，订单完成')
  load()
}

async function onRework() {
  const { value } = await ElMessageBox.prompt('请输入返修原因', '申请返修', { type: 'warning' })
  if (!value) return
  await reworkOrder(detail.value.id, { reason: value })
  ElMessage.success('返修申请已提交')
  load()
}

async function onDispute() {
  const { value } = await ElMessageBox.prompt('请描述发起仲裁的理由', '发起仲裁', { type: 'warning' })
  if (!value) return
  await ElMessageBox.confirm('确定对该订单发起仲裁吗？平台将介入裁定', '发起仲裁', { type: 'warning' })
  await disputeOrder(detail.value.id, { reasonType: 1, description: value, evidenceUrls: '' })
  ElMessage.success('仲裁申请已提交')
  load()
}

async function onReview() {
  await submitReview({ orderId: detail.value.id, score: reviewScore.value, content: reviewContent.value, tags: '', isAnonymous: 0 })
  ElMessage.success('评价成功')
  reviewed.value = true
}

async function copyText(t) {
  if (!t) return
  try { await navigator.clipboard.writeText(t) } catch (e) {}
  ElMessage.success('已复制')
}

onMounted(() => {
  load()
  timer = setInterval(() => { nowTick.value = Date.now() }, 1000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.detail-card { max-width: 1200px; }
.steps { margin-bottom: 20px; }
.title { margin: 0 0 6px; color: #18191c; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.desc { color: #61666d; font-size: 14px; margin: 0 0 14px; }
.desc-box { margin-top: 4px; }
.action-card { padding: 16px; }
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; margin-bottom: 10px; color: #18191c; }
.deliver-line { word-break: break-all; margin-bottom: 6px; }
</style>
