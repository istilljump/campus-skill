<template>
  <div class="profile-page">
    <!-- 体验账号提醒：未设置密码的账号有丢失风险，给出可操作引导 -->
    <el-alert v-if="!store.profile.username" type="warning" :closable="false" show-icon class="guest-alert">
      <template #title>
        当前为体验账号（未设置密码），仅本机可用。建议在下方「修改密码」中设置密码以便长期登录。
      </template>
    </el-alert>

    <!-- 身份 Hero：与首页同一渐变语言，作为"我的"页视觉锚点 -->
    <div class="page-card profile-hero">
      <div class="hero-avatar">{{ (store.profile.name || '校')[0] }}</div>
      <div class="hero-info">
        <div class="hero-name">
          {{ store.profile.name || '校园用户' }}
          <el-tag size="small" :type="creditTagType" effect="dark">{{ userLevel }}</el-tag>
          <el-tag v-if="!store.profile.username" size="small" type="warning" effect="plain">体验账号</el-tag>
        </div>
        <div class="muted" style="font-size:13px">
          ID {{ store.profile.id }} · {{ store.profile.campus || '校区未填写' }} · 登录账号 {{ store.profile.username || '未设置' }}
        </div>
      </div>
      <div class="hero-stats">
        <div class="hero-stat">
          <b>{{ store.profile.creditScore ?? 100 }}</b><span>信用分</span>
        </div>
        <div class="hero-stat">
          <b>{{ store.profile.publishOrderCount ?? 0 }}</b><span>累计发单</span>
        </div>
        <div class="hero-stat" @click="$router.push('/orders')">
          <b>{{ reviewTotal }}</b><span>我的评价</span>
        </div>
      </div>
    </div>

    <!-- 新手成长任务：把"引导"变成可勾选的清单，全部完成转为祝贺态 -->
    <div class="page-card" style="margin-bottom:16px">
      <div class="card-title">
        <el-icon><Flag /></el-icon><span>成长任务</span>
        <span class="muted" style="font-size:12px;font-weight:400">
          {{ tasks.every(t => t.done) ? '全部完成，你已经玩转 CampusSkill！🎉' : `已完成 ${tasks.filter(t => t.done).length}/${tasks.length}` }}
        </span>
      </div>
      <div class="task-grid">
        <div v-for="t in tasks" :key="t.key" class="task-item" :class="{ done: t.done }" @click="t.done ? null : t.go()">
          <span class="task-state">{{ t.done ? '✓' : t.icon }}</span>
          <div class="task-body">
            <div class="task-title">{{ t.title }}<el-tag v-if="t.done" size="small" type="success" effect="plain" style="margin-left:8px">已完成</el-tag></div>
            <div class="muted" style="font-size:12px">{{ t.desc }}</div>
          </div>
          <el-icon v-if="!t.done" class="task-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <div class="profile-layout">
      <div class="page-card">
        <div class="card-title"><el-icon><User /></el-icon><span>个人资料</span></div>
        <el-form :model="form" label-position="top" style="max-width:420px">
          <el-form-item label="昵称"><el-input v-model="form.name" maxlength="20" show-word-limit /></el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" maxlength="11" placeholder="11 位手机号" />
          </el-form-item>
          <el-form-item label="学号"><el-input v-model="form.studentNo" maxlength="20" /></el-form-item>
          <el-form-item label="校区"><el-input v-model="form.campus" maxlength="20" /></el-form-item>
          <el-button type="primary" style="width:100%" :loading="busy" @click="onSave">保存资料</el-button>
        </el-form>
      </div>

      <div class="side-col">
        <div class="page-card">
          <div class="card-title"><el-icon><Medal /></el-icon><span>账号信息</span></div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户 ID">{{ store.profile.id }}</el-descriptions-item>
            <el-descriptions-item label="登录账号">{{ store.profile.username || '（体验账号，未设置）' }}</el-descriptions-item>
            <el-descriptions-item label="信用分">
              <el-tag size="small" :type="creditTagType" effect="plain">{{ store.profile.creditScore ?? 100 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="累计发单">{{ store.profile.publishOrderCount ?? 0 }} 单</el-descriptions-item>
            <el-descriptions-item label="校区">{{ store.profile.campus || '未填写' }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="page-card">
          <div class="card-title"><el-icon><Lock /></el-icon><span>修改密码</span></div>
          <el-form label-position="top" style="max-width:420px">
            <el-form-item label="原密码（首次设置可不填）"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
            <el-form-item label="新密码（6-32位）">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
              <div v-if="pwdForm.newPassword" class="pwd-strength" :class="pwdStrength.cls">密码强度：{{ pwdStrength.label }}</div>
            </el-form-item>
            <el-form-item label="确认新密码"><el-input v-model="pwdForm.confirm" type="password" show-password /></el-form-item>
            <el-button type="primary" style="width:100%" :loading="busy" @click="onChangePwd">更新密码</el-button>
          </el-form>
        </div>
        <div class="page-card">
          <div class="card-title"><el-icon><Connection /></el-icon><span>快捷入口</span></div>
          <div class="quick-grid">
            <div class="quick-item" @click="$router.push('/wallet')">💰 我的钱包</div>
            <div class="quick-item" @click="$router.push('/market')">📅 我的预约</div>
            <div class="quick-item" @click="$router.push('/address')">📍 地址管理</div>
            <div class="quick-item" @click="$router.push('/apply-skiller')">🌱 成为技能者</div>
          </div>
          <el-button style="width:100%;margin-top:12px" @click="onLogout">退出登录</el-button>
        </div>
      </div>
    </div>

    <!-- 信用档案：用户信用等级说明 + 技能者信用流水 -->
    <div class="page-card" style="margin-top:16px">
      <div class="card-title"><el-icon><TrophyBase /></el-icon><span>信用档案</span></div>
      <div class="credit-top">
        <div class="credit-score">
          <span class="score-num">{{ store.profile.creditScore ?? 100 }}</span>
          <el-tag size="small" :type="creditTagType" effect="dark">{{ userLevel }}</el-tag>
        </div>
        <p class="muted credit-desc">{{ userLevelDesc }}</p>
      </div>
      <el-table :data="CREDIT_RULES" size="small" style="margin-top:8px">
        <el-table-column prop="action" label="行为" width="140" />
        <el-table-column prop="delta" label="信用分变动" width="120">
          <template #default="{ row }">
            <span :style="{ color: row.delta > 0 ? '#00b578' : '#ee0a24', fontWeight: 600 }">
              {{ row.delta > 0 ? '+' : '' }}{{ row.delta }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="说明" />
      </el-table>

      <template v-if="store.hasSkiller">
        <el-divider content-position="left">技能者信用流水（最近 8 条）</el-divider>
        <el-table :data="skCredit?.logs?.slice(0, 8) || []" size="small" v-loading="creditLoading">
          <el-table-column label="变动" width="90">
            <template #default="{ row }">
              <span :style="{ color: row.delta > 0 ? '#00b578' : '#ee0a24', fontWeight: 600 }">
                {{ row.delta > 0 ? '+' : '' }}{{ row.delta }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="160" />
          <el-table-column prop="orderNumber" label="关联订单" width="180">
            <template #default="{ row }">{{ row.orderNumber || '—' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="160" />
          <template #empty><el-empty description="暂无信用流水，接单履约后产生" :image-size="60" /></template>
        </el-table>
        <div v-if="skCredit" class="muted" style="font-size:12px;margin-top:6px">
          技能者信用 {{ skCredit.creditScore }}（{{ skCredit.level }}）· {{ skCredit.levelDesc }}
          <template v-if="skCredit.toNextLevel != null"> · 距下一等级还差 {{ skCredit.toNextLevel }} 分</template>
        </div>
      </template>
    </div>

    <!-- 我的评价 -->
    <div class="page-card" style="margin-top:16px">
      <div class="card-title"><el-icon><ChatDotRound /></el-icon><span>我的评价</span></div>
      <template v-if="myReviews.length">
        <div v-for="r in myReviews" :key="r.id" class="review-item" @click="$router.push(`/orders/${r.orderId}`)">
          <div class="review-head">
            <el-rate :model-value="r.score" disabled size="small" />
            <span class="muted" style="font-size:12px">{{ r.createTime }}</span>
          </div>
          <div class="review-content">{{ r.content || '（未填写文字评价）' }}</div>
          <div class="muted" style="font-size:12px">
            {{ r.orderTitle || '订单' }}<template v-if="r.isAnonymous === 1"> · 匿名</template>
            <span style="color:#00aeec;margin-left:8px">查看订单 ›</span>
          </div>
        </div>
      </template>
      <el-empty v-else description="还没有发出过评价，完成订单后可评价技能者" :image-size="72">
        <el-button type="primary" plain @click="$router.push('/orders')">去我的订单</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Medal, Lock, Connection, TrophyBase, ChatDotRound, Flag, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateProfile, getMyReviews, getSkillerCredit } from '@/api'
import { useRouter } from 'vue-router'

const store = useUserStore()
const router = useRouter()
const busy = ref(false)
const form = reactive({ name: '', phone: '', studentNo: '', campus: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const skCredit = ref(null)
const creditLoading = ref(false)
const myReviews = ref([])
const reviewTotal = ref(0)

// ===== 新手成长任务（数据全部来自现有接口，零后端改动）=====
const tasks = computed(() => [
  {
    key: 'password', icon: '🔑', done: !!store.profile.username,
    title: '设置登录密码', desc: '体验账号未设密码有丢失风险，设置后可长期登录',
    go: () => document.querySelector('.side-col .el-card, .side-col') && document.querySelectorAll('.side-col .page-card')[1]?.scrollIntoView({ behavior: 'smooth', block: 'center' }),
  },
  {
    key: 'firstOrder', icon: '📦', done: (store.profile.publishOrderCount ?? 0) > 0,
    title: '发出第一单悬赏', desc: '发布你的第一个技能需求，体验担保交易流程',
    go: () => router.push('/submit'),
  },
  {
    key: 'firstReview', icon: '⭐', done: reviewTotal.value > 0,
    title: '完成首次评价', desc: '订单完成后评价技能者，你的反馈帮大家选得更好',
    go: () => router.push('/orders'),
  },
  {
    key: 'skiller', icon: '🌱', done: store.hasSkiller,
    title: '入驻成为技能者', desc: '有设计/剪辑/编程等技能？认证后即可挂牌接单赚钱',
    go: () => router.push('/apply-skiller'),
  },
])

// ===== 密码强度反馈 =====
const pwdStrength = computed(() => {
  const p = pwdForm.newPassword || ''
  if (!p) return { label: '', cls: '' }
  const mixed = /[A-Za-z]/.test(p) && /\d/.test(p)
  if (p.length >= 10 && mixed) return { label: '强', cls: 'strong' }
  if (p.length >= 8 && (mixed || /[^A-Za-z0-9]/.test(p))) return { label: '中', cls: 'mid' }
  return { label: '弱：建议 8 位以上并混合字母与数字', cls: 'weak' }
})

const CREDIT_RULES = [
  { action: '交付完成', delta: 1, note: '每完成一单按时交付' },
  { action: '获得好评', delta: 2, note: '用户评价 4 星及以上' },
  { action: '返修', delta: -3, note: '用户验收不通过发起返修' },
  { action: '超时未交付', delta: -5, note: '同时扣除违约金' },
  { action: '仲裁判责', delta: -10, note: '平台仲裁判定技能者责任' },
]

const userLevel = computed(() => {
  const s = store.profile.creditScore ?? 100
  return s >= 90 ? 'C3' : s >= 70 ? 'C2' : 'C1'
})
const userLevelDesc = computed(() => ({
  C3: '信用优秀：可获更多曝光与信任',
  C2: '信用良好：保持按时交付可升级 C3',
  C1: '信用待提升：避免返修与超时',
})[userLevel.value])
const creditTagType = computed(() => (userLevel.value === 'C3' ? 'success' : userLevel.value === 'C2' ? 'primary' : 'warning'))

async function onSave() {
  if (!form.name || !form.name.trim()) return ElMessage.warning('昵称不能为空')
  if (form.phone && !/^1\d{10}$/.test(form.phone)) return ElMessage.warning('手机号格式不正确（11 位数字）')
  busy.value = true
  try {
    await updateProfile({ ...form })
    ElMessage.success('资料已更新')
    store.loadProfile()
  } finally { busy.value = false }
}

async function onChangePwd() {
  if (pwdForm.newPassword !== pwdForm.confirm) return ElMessage.warning('两次输入的新密码不一致')
  if (pwdForm.oldPassword && pwdForm.oldPassword === pwdForm.newPassword) return ElMessage.warning('新密码不能与原密码相同')
  busy.value = true
  try {
    await store.changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirm: '' })
    //安全惯例：改密成功后旧会话失效，强制重新登录
    await ElMessageBox.alert('密码已更新，请使用新密码重新登录', '修改成功', { type: 'success', confirmButtonText: '去登录' })
    await store.logout()
    router.push('/login')
  } finally { busy.value = false }
}

async function onLogout() {
  await ElMessageBox.confirm('确定退出登录吗？', '退出登录', { type: 'warning' })
  await store.logout()
  ElMessage.success('已退出')
  router.push('/login')
}

onMounted(async () => {
  await store.loadProfile()
  Object.assign(form, {
    name: store.profile.name, phone: store.profile.phone,
    studentNo: store.profile.studentNo, campus: store.profile.campus,
  })
  if (store.hasSkiller) {
    creditLoading.value = true
    try { skCredit.value = await getSkillerCredit() } catch { /* 技能者信用拉取失败静默 */ } finally { creditLoading.value = false }
  }
  try {
    const data = await getMyReviews(1, 3)
    myReviews.value = data?.records || []
    reviewTotal.value = Number(data?.total) || 0
  } catch { /* 评价拉取失败静默 */ }
})
</script>

<style scoped>
.guest-alert { margin-bottom: 16px; }

/* ===== 身份 Hero ===== */
.profile-hero {
  display: flex;
  align-items: center;
  gap: 16px;
  background: linear-gradient(135deg, rgba(0, 174, 236, .22), rgba(251, 114, 153, .16) 55%, rgba(14, 165, 233, .12));
  border: 1px solid rgba(0, 174, 236, .22);
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.hero-avatar {
  width: 52px; height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #00aeec, #0ea5e9);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; font-weight: 700;
  box-shadow: 0 4px 12px rgba(0, 174, 236, .35);
  flex-shrink: 0;
}
.hero-info { flex: 1; min-width: 200px; }
.hero-name { font-size: 17px; font-weight: 700; color: #18191c; display: flex; align-items: center; gap: 8px; }
.hero-stats { display: flex; gap: 0; background: rgba(255, 255, 255, .72); border-radius: 12px; padding: 10px 0; }
.hero-stat {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 0 22px;
  cursor: pointer;
}
.hero-stat + .hero-stat { border-left: 1px solid rgba(0, 0, 0, .06); }
.hero-stat b { font-size: 17px; color: #18191c; }
.hero-stat span { font-size: 12px; color: #9499a0; }
.hero-stat:hover { opacity: .75; }

.card-title .el-icon { color: #00aeec; }
.profile-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
@media (max-width: 1000px) { .profile-layout { grid-template-columns: 1fr; } }
.side-col { display: grid; gap: 16px; }
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; margin-bottom: 12px; color: #18191c; }
.quick-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.quick-item {
  padding: 12px 14px;
  border: 1px solid #e3e5e7;
  border-radius: 12px;
  cursor: pointer;
  color: #61666d;
  font-size: 14px;
  transition: transform .16s ease, border-color .16s ease, background .16s ease;
}
.quick-item:hover { background: #f6f7f8; border-color: rgba(0, 174, 236, .45); transform: translateY(-2px); }

.credit-top { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.credit-score { display: flex; align-items: center; gap: 10px; }
.score-num { font-size: 34px; font-weight: 700; color: #18191c; }
.credit-desc { margin: 0; font-size: 13px; }

.review-item {
  border: 1px solid #e3e5e7;
  border-radius: 12px;
  padding: 12px 14px;
  cursor: pointer;
  margin-bottom: 10px;
  transition: border-color .16s ease, transform .16s ease;
}
.review-item:hover { border-color: rgba(0, 174, 236, .45); transform: translateY(-2px); }
.review-head { display: flex; justify-content: space-between; align-items: center; }
.review-content { font-size: 13px; color: #61666d; margin: 6px 0; }

.task-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
@media (max-width: 900px) { .task-grid { grid-template-columns: 1fr; } }
.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #e3e5e7;
  border-radius: 12px;
  padding: 12px 14px;
  cursor: pointer;
  transition: border-color .16s ease, background .16s ease;
}
.task-item:hover { border-color: rgba(0, 174, 236, .45); background: #f6f7f8; }
.task-item.done { cursor: default; opacity: .75; border-color: #d1f0dd; background: #f7fcf9; }
.task-item.done:hover { transform: none; }
.task-state {
  width: 34px; height: 34px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  background: #f1f3f5;
  font-size: 16px;
  flex-shrink: 0;
}
.task-item.done .task-state { background: #00b578; color: #fff; font-size: 14px; }
.task-body { flex: 1; min-width: 0; }
.task-title { font-size: 14px; font-weight: 600; color: #18191c; }
.task-arrow { color: #c9ccd0; }

.pwd-strength { font-size: 12px; margin-top: 4px; line-height: 1.4; }
.pwd-strength.weak { color: #ee0a24; }
.pwd-strength.mid { color: #ff976a; }
.pwd-strength.strong { color: #00b578; }
</style>
