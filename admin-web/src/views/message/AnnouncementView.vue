<template>
  <div class="announcement-layout">
    <!-- 左：发布表单 -->
    <div class="page-card compose-card">
      <div class="card-title">
        <el-icon><Promotion /></el-icon>
        <span>发布系统公告</span>
      </div>
      <p class="muted">公告将实时推送到全体用户与技能者的消息中心（在线用户立即弹窗提醒）。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" maxlength="64" show-word-limit placeholder="如：平台周末服务费 5 折优惠" />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" maxlength="500" show-word-limit
            placeholder="写清楚公告的具体内容、生效时间与适用范围" />
        </el-form-item>
        <el-button type="primary" class="publish-btn" :loading="publishing" @click="onPublish">
          发布公告
        </el-button>
      </el-form>
    </div>

    <!-- 右：公告历史 -->
    <div class="page-card history-card">
      <div class="card-title">
        <el-icon><Bell /></el-icon>
        <span>公告历史</span>
        <el-button size="small" text :icon="Refresh" class="refresh-btn" @click="load">刷新</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="送达" width="90">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.recipientCount }} 人</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="adminName" label="发布人" width="100" />
      </el-table>
      <div class="table-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          v-model:current-page="page" :page-size="pageSize" @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { broadcastAnnouncement, pageAnnouncements } from '@/api'
import { fmtTime } from '@/utils/dict'

const formRef = ref()
const publishing = ref(false)
const form = reactive({ title: '', content: '' })
const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
}

const list = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const data = await pageAnnouncements({ page: page.value, pageSize })
    list.value = data.records || []
    total.value = Number(data.total) || 0
  } finally {
    loading.value = false
  }
}

async function onPublish() {
  await formRef.value.validate()
  // 二次确认：广播会送达全部用户与技能者，属外向型操作
  await ElMessageBox.confirm(
    '公告将推送给平台全部用户与技能者，确认发布吗？',
    '发布确认',
    { type: 'warning', confirmButtonText: '确认发布', cancelButtonText: '再改改' },
  )
  publishing.value = true
  try {
    await broadcastAnnouncement({ ...form })
    ElMessage.success('公告已发布并推送')
    form.title = ''
    form.content = ''
    formRef.value.resetFields()
    page.value = 1
    load()
  } finally {
    publishing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
/* 桌面双栏：左发布（固定宽）+ 右历史（自适应） */
.announcement-layout {
  display: grid;
  grid-template-columns: 400px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

@media (max-width: 1100px) {
  .announcement-layout {
    grid-template-columns: 1fr;
  }
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #18191c;
}

.refresh-btn {
  margin-left: auto;
  color: #00aeec;
}

.publish-btn {
  width: 100%;
}
</style>
