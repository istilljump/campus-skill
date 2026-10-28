<template>
  <div class="page-card submit-card">
    <div class="card-title">
      <el-icon><EditPen /></el-icon>
      <span>发布「{{ curType.name || '技能' }}」悬赏</span>
      <el-tag v-if="curType.feeRate != null" type="primary" effect="plain">服务费率 {{ (curType.feeRate * 100).toFixed(0) }}%</el-tag>
    </div>

    <el-form :model="form" label-position="top" class="submit-form">
      <el-form-item label="服务类目" required>
        <el-select v-model="form.typeId" style="width:100%" placeholder="选择技能类目" @change="onTypeChange">
          <el-option v-for="t in types" :key="t.id" :value="t.id" :label="t.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="服务标题" required>
        <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="如：课程汇报PPT美化（20页内）" />
      </el-form-item>
      <el-form-item label="需求描述">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="写清交付物形式、修改轮次等（选填）" />
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="服务/取件地点（线下服务填写，选填）">
            <el-input v-model="form.pickupAddress" placeholder="如：东区图书馆" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="期望完成时间（选填）">
            <el-date-picker v-model="expected" type="datetime" style="width:100%"
              value-format="YYYY-MM-DDTHH:mm" placeholder="选择日期时间" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="交付方式">
        <el-radio-group v-model="form.addrMode">
          <el-radio-button value="online">线上交付</el-radio-button>
          <el-radio-button value="book">地址簿</el-radio-button>
          <el-radio-button value="manual">手动填写</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="form.addrMode === 'book'" label="交付地址">
        <el-select v-model="form.addressBookId" style="width:100%" placeholder="选择地址簿地址">
          <el-option v-for="a in addresses" :key="a.id" :value="a.id"
            :label="`${a.campus} ${a.building} ${a.room}（${a.consignee}）`" />
        </el-select>
        <div class="muted" style="margin-top:6px">还没有地址？去 <el-link type="primary" @click="$router.push('/address')">地址管理</el-link> 新增</div>
      </el-form-item>
      <el-form-item v-if="form.addrMode === 'manual'" label="交付地点" required>
        <el-input v-model="form.manualAddress" placeholder="如：东区3号楼502" />
      </el-form-item>

      <el-form-item label="悬赏金额（元）" required>
        <div class="amount-row">
          <el-input-number v-model="form.rewardAmount" :min="1" :max="5000" :precision="0" style="width:200px" />
          <el-tag v-for="a in [20, 50, 100, 200]" :key="a" size="large" class="amount-tag"
            :type="form.rewardAmount === a ? 'primary' : 'info'" @click="form.rewardAmount = a">¥{{ a }}</el-tag>
        </div>
        <!-- 智能定价参考：类目历史成交价分位区间 + 当前金额落点 -->
        <div v-if="priceRef && priceRef.sampleCount > 0" class="price-ref">
          <div class="pr-track-row">
            <span class="pr-tick">¥{{ priceRef.p25 }}</span>
            <div class="pr-track">
              <div class="pr-band"></div>
              <div class="pr-marker pr-p50" :style="{ left: posPct(priceRef.p50) + '%' }"></div>
              <div class="pr-dot" :class="{ out: amountOut }" :style="{ left: amountPos + '%' }"></div>
            </div>
            <span class="pr-tick">¥{{ priceRef.p75 }}</span>
          </div>
          <div class="pr-meta">
            近 {{ priceRef.sampleCount }} 单成交 · 中位 ¥{{ priceRef.p50 }}<template v-if="priceRef.avgDeliveryHours"> · 平均 {{ priceRef.avgDeliveryHours }}h 交付</template>
          </div>
          <div v-if="amountHint" class="pr-hint" :class="amountHint.type">{{ amountHint.text }}</div>
        </div>
      </el-form-item>

      <el-alert v-if="form.rewardAmount > 0" type="info" :closable="false" class="fee-alert">
        平台服务费 {{ fmtMoney(form.rewardAmount * feeRateNum) }} · 技能者实得 {{ fmtMoney(form.rewardAmount * (1 - feeRateNum)) }}
      </el-alert>

      <el-form-item label="支付方式" style="margin-top:8px">
        <el-radio-group v-model="form.payMethod">
          <el-radio :value="2">钱包余额</el-radio>
          <el-radio :value="1" disabled>微信支付（演示环境降级为余额）</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-button type="primary" size="large" style="width:100%" :loading="busy" @click="onSubmit">立即发单</el-button>
    </el-form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import { submitOrder, listCategories, listAddresses, getPriceReference } from '@/api'
import { fmtMoney } from '@/utils/dict'

const router = useRouter()
const types = ref([])
const addresses = ref([])
const busy = ref(false)
const expected = ref('')
const curType = ref({})
const priceRef = ref(null)
const form = reactive({
  title: '', description: '', pickupAddress: '', addressBookId: null,
  rewardAmount: null, payMethod: 2, typeId: null, addrMode: 'online', manualAddress: '',
})

// 本地草稿暂存：与 H5 端一致，填写中途退出不丢内容
const DRAFT_KEY = 'web_draft_order'

const feeRateNum = computed(() => Number(curType.value.feeRate) || 0.1)

// ===== 智能定价参考：随类目拉取历史成交分位，当前金额超出 P25~P75 时柔性提示 =====
async function loadPriceRef() {
  if (!curType.value.id) { priceRef.value = null; return }
  try {
    priceRef.value = await getPriceReference(curType.value.id)
  } catch (e) {
    priceRef.value = null
  }
}

function onTypeChange(id) {
  const t = types.value.find((x) => x.id === id)
  if (t) curType.value = t
}

watch(() => curType.value.id, loadPriceRef)

// 参考条刻度：以 P25~P75 为常见区间，两端各外扩半个区间宽，便于容纳区间外金额的落点
const prLo = computed(() => {
  const r = priceRef.value
  if (!r || !r.sampleCount) return 0
  const p25 = Number(r.p25); const p75 = Number(r.p75)
  return Math.max(0, p25 - (p75 - p25) / 2)
})
const prHi = computed(() => {
  const r = priceRef.value
  if (!r || !r.sampleCount) return 1
  const p25 = Number(r.p25); const p75 = Number(r.p75)
  return p75 + (p75 - p25) / 2 || 1
})

function posPct(v) {
  if (!prHi.value) return 50
  return Math.max(0, Math.min(100, ((Number(v) - prLo.value) / (prHi.value - prLo.value)) * 100))
}

const amountPos = computed(() => {
  const a = Number(form.rewardAmount)
  if (!a) return 50
  return posPct(a)
})

const amountOut = computed(() => {
  const a = Number(form.rewardAmount); const r = priceRef.value
  return !!(r && r.sampleCount && a && (a < Number(r.p25) || a > Number(r.p75)))
})

const amountHint = computed(() => {
  const a = Number(form.rewardAmount); const r = priceRef.value
  if (!r || !r.sampleCount || !a) return null
  if (a > Number(r.p75)) return { type: 'warn', text: `高于同类 75% 成交价（P75 ¥${r.p75}），接单可能变慢，可考虑下调` }
  if (a < Number(r.p25)) return { type: 'info', text: `低于同类常见区间（P25 ¥${r.p25}），更容易被快速接单` }
  return { type: 'ok', text: '处于同类常见成交区间内，接单速度与性价比均衡' }
})

// 草稿自动暂存（与 H5 端行为一致）：填写中途退出不丢内容，提交成功后清除
watch([form, expected], () => {
  localStorage.setItem(DRAFT_KEY, JSON.stringify({ ...form, expectedTime: expected.value || '' }))
}, { deep: true })

async function onSubmit() {
  if (!form.title.trim()) return ElMessage.warning('请填写订单标题')
  if (form.addrMode === 'book' && !form.addressBookId) return ElMessage.warning('请先选择或新增交付地址')
  if (form.addrMode === 'manual' && !(form.manualAddress || '').trim()) return ElMessage.warning('请填写交付地点')
  if (!form.rewardAmount || form.rewardAmount <= 0) return ElMessage.warning('请填写正确的悬赏金额')
  busy.value = true
  try {
    // 组装 payload：与 H5 端 submitOrder 逻辑一致
    const payload = { ...form, typeId: curType.value.id, expectedTime: '' }
    if (payload.addrMode === 'online') { payload.deliveryAddress = '线上交付'; delete payload.addressBookId }
    if (payload.addrMode === 'book') { delete payload.deliveryAddress; delete payload.manualAddress }
    if (payload.addrMode === 'manual') { payload.deliveryAddress = payload.manualAddress; delete payload.addressBookId }
    delete payload.addrMode
    delete payload.manualAddress
    if (expected.value) payload.expectedTime = String(expected.value).replace('T', ' ') + ':00'
    else delete payload.expectedTime
    const r = await submitOrder(payload)
    localStorage.removeItem(DRAFT_KEY)
    ElMessage.success('发单成功')
    if (form.payMethod === 2) {
      const { payOrder } = await import('@/api')
      await payOrder(r.orderNumber).catch(() => {})
    }
    router.push('/orders')
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  types.value = await listCategories() || []
  addresses.value = await listAddresses() || []
  if (addresses.value.length) form.addressBookId = addresses.value[0].id
  // 默认选中第一个类目（与 H5 startOrder 行为对齐：从概览快捷入口进入时也可手动切换）
  if (types.value.length) {
    curType.value = types.value[0]
    form.typeId = types.value[0].id
  }
  try {
    const draft = JSON.parse(localStorage.getItem(DRAFT_KEY) || 'null')
    if (draft && (draft.title || draft.rewardAmount)) {
      Object.assign(form, draft)
      // 草稿里的类目要同步回 curType：提交 payload 与价格参考都以 curType 为准
      const t = types.value.find((x) => x.id === form.typeId)
      if (t) curType.value = t
      if (draft.expectedTime) expected.value = draft.expectedTime.replace(' ', 'T').slice(0, 16)
      ElMessage.info('已恢复上次未提交的草稿')
    }
  } catch (e) {}
})
</script>

<style scoped>
.submit-card { max-width: 860px; }
.card-title { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; margin-bottom: 16px; color: #18191c; }
.amount-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.amount-tag { cursor: pointer; }
.fee-alert { margin-bottom: 16px; }
/* 智能定价参考条 */
.price-ref { width: 100%; margin-top: 10px; padding: 10px 12px; background: #f7fbfd; border: 1px solid #e0f0f7; border-radius: 8px; }
.pr-track-row { display: flex; align-items: center; gap: 8px; }
.pr-tick { font-size: 12px; color: #61666d; font-variant-numeric: tabular-nums; min-width: 34px; }
.pr-tick:last-child { text-align: right; }
.pr-track { position: relative; flex: 1; height: 8px; background: #eef1f3; border-radius: 4px; }
.pr-band { position: absolute; left: 25%; right: 25%; top: 0; bottom: 0; border-radius: 4px;
  background: linear-gradient(90deg, #b3e7f9, #4dc3f2); }
.pr-marker { position: absolute; top: -3px; width: 2px; height: 14px; background: #0093cf; transform: translateX(-1px); }
.pr-dot { position: absolute; top: 50%; width: 12px; height: 12px; border-radius: 50%;
  background: #00aeec; border: 2px solid #fff; box-shadow: 0 1px 4px rgba(0, 174, 236, .5);
  transform: translate(-50%, -50%); transition: left .2s ease; }
.pr-dot.out { background: #ff976a; box-shadow: 0 1px 4px rgba(255, 151, 106, .55); }
.pr-meta { margin-top: 6px; font-size: 12px; color: #9499a0; }
.pr-hint { margin-top: 4px; font-size: 12px; }
.pr-hint.warn { color: #d98200; }
.pr-hint.info { color: #0093cf; }
.pr-hint.ok { color: #1f9e50; }
</style>
