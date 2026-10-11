<template>
  <div class="home">
    <!-- 主视觉：核心数据快速呈现（与 H5 首页 hero 一致） -->
    <div class="hero">
      <div>
        <h2>Hi，让技能帮你搞定</h2>
        <p>信用分 {{ store.profile.creditScore ?? 100 }} · 已发单 {{ store.profile.publishOrderCount ?? 0 }} 单
          · 钱包余额 {{ fmtMoney(wallet.balance) }}</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" :icon="EditPen" @click="$router.push('/submit')">发布悬赏</el-button>
        <el-button :icon="ShoppingCart" @click="$router.push('/market')">逛技能市场</el-button>
      </div>
    </div>

    <div class="home-grid">
      <!-- 进行中的订单 -->
      <div class="page-card span-2">
        <div class="card-title">
          <el-icon><Tickets /></el-icon><span>进行中的订单</span>
          <el-button size="small" text :icon="Refresh" class="refresh-btn" @click="loadActive">刷新</el-button>
        </div>
        <el-table :data="activeOrders" v-loading="loading" stripe>
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="typeName" label="类型" width="100" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="ORDER_STATUS[row.status]?.type">{{ ORDER_STATUS[row.status]?.label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="悬赏" width="100">
            <template #default="{ row }"><span class="money">{{ fmtMoney(row.rewardAmount) }}</span></template>
          </el-table-column>
          <el-table-column label="发布时间" width="150">
            <template #default="{ row }">{{ relTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="$router.push(`/orders/${row.id}`)">进度</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无进行中的订单，发一单试试吧" :image-size="72" /></template>
        </el-table>
      </div>

      <!-- 快捷入口 + 成为技能者 -->
      <div class="side-col">
        <div class="page-card">
          <div class="card-title"><el-icon><Compass /></el-icon><span>快捷入口</span></div>
          <div class="quick-grid">
            <div class="quick-item" @click="$router.push('/hall')"><span class="ico">🧭</span>悬赏大厅</div>
            <div class="quick-item" @click="$router.push('/market')"><span class="ico">🛒</span>技能市场</div>
            <div class="quick-item" @click="$router.push('/orders')"><span class="ico">🧾</span>我的订单</div>
            <div class="quick-item" @click="$router.push('/wallet')"><span class="ico">💰</span>我的钱包</div>
            <div class="quick-item" @click="$router.push('/messages')"><span class="ico">🔔</span>消息中心<el-badge v-if="store.unread > 0" :value="store.unread" :max="99" class="quick-badge" /></div>
            <div class="quick-item" @click="$router.push('/address')"><span class="ico">📍</span>地址管理</div>
          </div>
        </div>
        <div class="page-card" v-if="!store.hasSkiller">
          <div class="card-title"><el-icon><Star /></el-icon><span>成为技能者</span></div>
          <p class="muted">有设计、剪辑、编程等技能？提交入驻申请，审核通过后即可挂牌接单。</p>
          <el-button type="primary" plain style="width:100%" @click="$router.push('/apply-skiller')">去申请入驻</el-button>
        </div>
        <div class="page-card" v-else>
          <div class="card-title"><el-icon><Opportunity /></el-icon><span>技能者工作台</span></div>
          <p class="muted">你已是认证技能者，可直接前往接单台抢单、管理服务与作品。</p>
          <el-button type="primary" plain style="width:100%" @click="$router.push('/sk/orders')">进入接单台</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { EditPen, ShoppingCart, Refresh } from '@element-plus/icons-vue'
import { pageOrders, getWallet } from '@/api'
import { useUserStore } from '@/stores/user'
import { ORDER_STATUS, fmtMoney, relTime } from '@/utils/dict'

const store = useUserStore()
const activeOrders = ref([])
const wallet = ref({})
const loading = ref(false)

async function loadActive() {
  loading.value = true
  try {
    const data = await pageOrders({ page: 1, pageSize: 10 })
    activeOrders.value = (data.records || []).filter(o => [2, 3, 4].includes(o.status)).slice(0, 5)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  loadActive()
  store.loadProfile()
  wallet.value = await getWallet() || {}
})
</script>

<style scoped>
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, rgba(79, 140, 255, .28), rgba(124, 92, 255, .20) 55%, rgba(14, 165, 233, .14));
  border: 1px solid rgba(255, 255, 255, .18);
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 16px;
}

.hero h2 { margin: 0 0 6px; color: #eef3fb; }
.hero p { margin: 0; color: rgba(230, 239, 247, .82); font-size: 14px; }
.hero-actions { display: flex; gap: 4px; }

.home-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: 16px;
  align-items: start;
}

.span-2 { grid-column: span 1; }

.side-col { display: grid; gap: 16px; }

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #e6edf7;
}

.refresh-btn { margin-left: auto; color: #8fb8ff; }

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.quick-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, .1);
  border-radius: 12px;
  cursor: pointer;
  color: #c3cede;
  font-size: 14px;
  transition: transform .16s ease, border-color .16s ease, background .16s ease;
}

.quick-item:hover { background: rgba(255, 255, 255, .06); border-color: rgba(255, 255, 255, .2); transform: translateY(-2px); }
.quick-badge { position: absolute; top: -6px; right: -6px; }

@media (max-width: 1100px) {
  .home-grid { grid-template-columns: 1fr; }
  .hero { flex-direction: column; align-items: flex-start; gap: 12px; }
}
</style>
