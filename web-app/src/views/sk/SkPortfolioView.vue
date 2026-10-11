<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="card-title"><el-icon><Picture /></el-icon><span>我的作品</span></div>
      <el-button type="primary" :icon="Plus" @click="formVisible = true">发布作品</el-button>
    </div>

    <div class="pf-grid" v-loading="loading">
      <div v-for="p in portfolios" :key="p.id" class="mini-card">
        <img v-if="p.coverUrl" :src="p.coverUrl" class="cover" @error="e => e.target.style.display = 'none'" />
        <div class="card-head">
          <span class="title">{{ p.title }}</span>
          <el-tag :type="PORTFOLIO_STATUS[p.status]?.type" size="small">{{ PORTFOLIO_STATUS[p.status]?.label }}</el-tag>
        </div>
        <div v-if="p.description" class="muted meta">{{ p.description }}</div>
        <div v-if="p.status === 2 && p.auditOpinion" class="reject">驳回意见：{{ p.auditOpinion }}</div>
        <div class="card-foot">
          <span class="muted">{{ p.categoryName || '' }}</span>
          <el-button size="small" plain type="danger" @click="onDel(p)">删除</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && !portfolios.length" description="还没有作品，快发布第一个吧" style="grid-column:1/-1" />
    </div>

    <!-- 发布作品弹窗 -->
    <el-dialog v-model="formVisible" title="发布作品" width="520px">
      <el-form :model="form" label-position="top">
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
            <el-option v-for="c in cats" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作品标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="封面图 URL（选填）"><el-input v-model="form.coverUrl" /></el-form-item>
        <el-form-item label="作品图/链接（逗号分隔）"><el-input v-model="form.workUrls" /></el-form-item>
        <el-form-item label="作品描述（选填）">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <el-button type="primary" style="width:100%" :loading="busy" @click="onSubmit">提交审核</el-button>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { skListPortfolio, skAddPortfolio, skDelPortfolio, skListCategories } from '@/api'
import { PORTFOLIO_STATUS } from '@/utils/dict'

const portfolios = ref([])
const cats = ref([])
const loading = ref(false)
const busy = ref(false)
const formVisible = ref(false)
const form = reactive({ categoryId: null, title: '', coverUrl: '', workUrls: '', description: '' })

async function load() {
  loading.value = true
  try {
    const data = await skListPortfolio()
    portfolios.value = Array.isArray(data) ? data : (data?.records || [])
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!form.title) return ElMessage.warning('请填写作品标题')
  busy.value = true
  try {
    await skAddPortfolio({ ...form })
    ElMessage.success('已提交审核')
    formVisible.value = false
    Object.assign(form, { categoryId: null, title: '', coverUrl: '', workUrls: '', description: '' })
    load()
  } finally { busy.value = false }
}

async function onDel(p) {
  await ElMessageBox.confirm('确定删除该作品吗？', '删除作品', { type: 'warning' })
  await skDelPortfolio(p.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  cats.value = await skListCategories() || []
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #e6edf7; }
.pf-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
  min-height: 120px;
}
.mini-card {
  border: 1px solid rgba(255, 255, 255, .1);
  background: linear-gradient(160deg, rgba(255,255,255,.07), rgba(255,255,255,.04) 55%);
  border-radius: 14px;
  padding: 14px;
}
.cover { width: 100%; border-radius: 10px; margin-bottom: 10px; max-height: 180px; object-fit: cover; }
.card-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.title { font-weight: 600; color: #e6edf7; }
.meta { margin: 8px 0; font-size: 13px; }
.reject { color: var(--el-color-danger); font-size: 12px; margin-top: 6px; }
.card-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
</style>
