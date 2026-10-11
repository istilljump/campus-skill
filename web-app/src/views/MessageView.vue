<template>
  <div class="page-card">
    <div class="card-title">
      <el-icon><Bell /></el-icon><span>消息中心</span>
      <el-button size="small" plain type="primary" :loading="busy" @click="onReadAll">全部已读</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe @row-click="openOrder" :row-class-name="rowClass" style="cursor:pointer">
      <el-table-column label="标题" width="220">
        <template #default="{ row }">
          <el-badge is-dot :hidden="row.isRead !== 0" class="msg-dot">{{ row.title }}</el-badge>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="320" show-overflow-tooltip />
      <el-table-column label="时间" width="150">
        <template #default="{ row }">{{ relTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-link v-if="row.orderId" type="primary">查看订单 ›</el-link>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无消息" :image-size="72" /></template>
    </el-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listMessages, readAllMessages, skListMessages, skReadAllMessages } from '@/api'
import { relTime } from '@/utils/dict'

// 消息组件按路由分区复用：user 区走 /user/message，sk 区走 /skiller/message
const router = useRouter()
const store = useUserStore()
const list = ref([])
const loading = ref(false)
const busy = ref(false)
const isSk = computed(() => router.currentRoute.value.meta.area === 'sk')

async function load() {
  loading.value = true
  try {
    list.value = (isSk.value ? await skListMessages() : await listMessages()) || []
    store.loadUnread(isSk.value ? 'sk' : 'user')
  } finally {
    loading.value = false
  }
}

async function onReadAll() {
  busy.value = true
  try {
    isSk.value ? await skReadAllMessages() : await readAllMessages()
    ElMessage.success('已全部标记为已读')
    load()
  } finally { busy.value = false }
}

function rowClass({ row }) {
  return row.isRead === 0 ? 'unread-row' : ''
}

function openOrder(row) {
  if (row.orderId && !isSk.value) router.push(`/orders/${row.orderId}`)
}

onMounted(load)
</script>

<style scoped>
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; margin-bottom: 12px; color: #e6edf7; }
.card-title .el-button { margin-left: auto; }
:deep(.unread-row) { background: rgba(79, 140, 255, .08) !important; }
.msg-dot :deep(.el-badge__content) { display: none; }
.msg-dot { display: inline-block; }
.msg-dot.is-dot::after { content: ''; }
</style>
