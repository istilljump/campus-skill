<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="card-title"><el-icon><Goods /></el-icon><span>我的服务</span></div>
      <el-button type="primary" :icon="Plus" @click="openForm()">新增服务</el-button>
    </div>

    <el-table :data="services" v-loading="loading" stripe>
      <el-table-column prop="title" label="服务标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column label="价格" width="100">
        <template #default="{ row }"><span class="money">{{ fmtMoney(row.price) }}</span></template>
      </el-table-column>
      <el-table-column label="交付/方式" width="150">
        <template #default="{ row }">{{ row.deliveryDays }}天 · {{ modeText(row.serviceMode) }}</template>
      </el-table-column>
      <el-table-column prop="tags" label="标签" min-width="120" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架中' : '已下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" plain @click="onToggle(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
          <el-button size="small" type="primary" plain @click="openForm(row)">编辑</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="还没有服务，点击右上角新增" :image-size="72" /></template>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑服务' : '新增服务'" width="520px">
      <el-form :model="form" label-position="top">
        <el-form-item label="分类" required>
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
            <el-option v-for="c in cats" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="服务标题" required>
          <el-input v-model="form.title" placeholder="如：课程汇报PPT美化（20页内）" />
        </el-form-item>
        <el-form-item label="服务描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="价格（元）" required>
              <el-input-number v-model="form.price" :min="1" :max="5000" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="交付天数">
              <el-input-number v-model="form.deliveryDays" :min="1" :max="30" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="服务方式">
          <el-radio-group v-model="form.serviceMode">
            <el-radio :value="1">线上交付</el-radio>
            <el-radio :value="2">线下进行</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="逗号分隔，如：PPT,设计" />
        </el-form-item>
      </el-form>
      <el-button type="primary" style="width:100%" :loading="busy" @click="onSave">保存</el-button>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { skListServices, skAddService, skUpdateService, skToggleService, skListCategories } from '@/api'
import { fmtMoney, modeText } from '@/utils/dict'

const services = ref([])
const cats = ref([])
const loading = ref(false)
const busy = ref(false)
const formVisible = ref(false)
const form = reactive({ id: null, categoryId: null, title: '', description: '', price: null, deliveryDays: 1, serviceMode: 1, tags: '' })

async function load() {
  loading.value = true
  try {
    const data = await skListServices()
    services.value = Array.isArray(data) ? data : (data?.records || [])
  } finally {
    loading.value = false
  }
}

function openForm(s) {
  if (s) {
    Object.assign(form, { id: s.id, categoryId: s.categoryId, title: s.title, description: s.description || '',
      price: s.price, deliveryDays: s.deliveryDays || 1, serviceMode: s.serviceMode || 1, tags: s.tags || '' })
  } else {
    Object.assign(form, { id: null, categoryId: null, title: '', description: '', price: null, deliveryDays: 1, serviceMode: 1, tags: '' })
  }
  formVisible.value = true
}

async function onSave() {
  if (!form.categoryId) return ElMessage.warning('请选择分类')
  if (!form.title) return ElMessage.warning('请填写服务标题')
  if (!form.price || form.price <= 0) return ElMessage.warning('请填写正确的价格')
  busy.value = true
  try {
    if (form.id) await skUpdateService(form.id, { ...form })
    else await skAddService({ ...form })
    ElMessage.success('已保存')
    formVisible.value = false
    load()
  } finally { busy.value = false }
}

async function onToggle(row) {
  const to = row.status === 1 ? 0 : 1
  await skToggleService(row.id, to)
  ElMessage.success(to === 1 ? '已上架' : '已下架')
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
</style>
