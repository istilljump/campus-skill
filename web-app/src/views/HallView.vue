<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索标题 / 订单号，回车搜索" clearable style="width: 260px" @keyup.enter="onSearch" />
      <el-select v-model="typeId" placeholder="技能类目" clearable style="width: 140px" @change="onSearch">
        <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
      <span class="muted tip">大厅展示所有待接单悬赏；抢单请前往「技能者工作台 → 接单台」</span>
    </div>

    <div class="hall-grid" v-loading="loading">
      <div v-for="o in list" :key="o.id" class="hall-card">
        <div class="hall-head">
          <span class="title">{{ o.title }}</span>
          <span class="money">{{ fmtMoney(o.rewardAmount) }}</span>
        </div>
        <div class="muted route-line">{{ o.pickupAddress || '线上交付' }} → {{ o.deliveryAddress || '线上交付' }}</div>
        <div class="hall-foot">
          <span class="muted">{{ o.typeName }} · {{ o.publisherName }} 发布于 {{ relTime(o.createTime) }}</span>
        </div>
      </div>
      <el-empty v-if="!loading && !list.length" description="大厅暂无待接单订单" style="grid-column: 1/-1" />
    </div>

    <div class="table-pager">
      <el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="page" :page-size="pageSize" @change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { listHall, listCategories } from '@/api'
import { fmtMoney, relTime } from '@/utils/dict'

const keyword = ref('')
const typeId = ref(null)
const types = ref([])
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 12
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize }
    if (typeId.value) params.typeId = typeId.value
    if (keyword.value) params.keyword = keyword.value
    const data = await listHall(params)
    list.value = data.records || []
    total.value = Number(data.total) || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}

onMounted(async () => {
  load()
  types.value = await listCategories() || []
})
</script>

<style scoped>
.tip { margin-left: auto; }

/* 桌面多列卡片网格：随分辨率自适应列数 */
.hall-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
  min-height: 120px;
}

.hall-card {
  border: 1px solid rgba(255, 255, 255, .1);
  background: linear-gradient(160deg, rgba(255,255,255,.07), rgba(255,255,255,.04) 55%);
  border-radius: 14px;
  padding: 16px;
  transition: transform .16s ease, border-color .16s ease;
}

.hall-card:hover { transform: translateY(-2px); border-color: rgba(255, 255, 255, .22); }

.hall-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.title { font-weight: 600; color: #e6edf7; }
.route-line { margin: 8px 0; font-size: 13px; }
.hall-foot { display: flex; justify-content: space-between; font-size: 12px; }
</style>
