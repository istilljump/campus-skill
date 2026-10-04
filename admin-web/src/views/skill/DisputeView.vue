<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="仲裁状态" clearable style="width: 160px">
        <el-option v-for="(v, k) in DISPUTE_STATUS" :key="k" :label="v.label" :value="Number(k)" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="orderNumber" label="订单号" width="190" />
      <el-table-column prop="orderTitle" label="订单标题" min-width="140" show-overflow-tooltip />
      <el-table-column prop="userName" label="用户" width="110" />
      <el-table-column label="仲裁原因" width="100">
        <template #default="{ row }">{{ DISPUTE_REASON_TYPE[row.reasonType] || row.reasonType }}</template>
      </el-table-column>
      <el-table-column prop="description" label="问题描述" min-width="150" show-overflow-tooltip />
      <el-table-column label="凭证" min-width="110">
        <template #default="{ row }">
          <template v-if="parseEvidence(row.evidenceUrls).length">
            <el-link v-for="(u, i) in parseEvidence(row.evidenceUrls)" :key="i" type="primary" :href="u" target="_blank"
              style="margin-right: 8px">凭证{{ i + 1 }}</el-link>
          </template>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="DISPUTE_STATUS[row.status]?.type">{{ DISPUTE_STATUS[row.status]?.label || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="verdict" label="仲裁结论" min-width="120" show-overflow-tooltip />
      <el-table-column label="发起时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" link type="primary" @click="openVerdict(row)">仲裁</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pager">
      <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.pageSize" :page-sizes="[10, 20, 50]"
        @change="load" />
    </div>

    <el-dialog v-model="verdictVisible" title="仲裁处理" width="460px">
      <el-form ref="formRef" :model="verdictForm" :rules="rules" label-width="80px">
        <el-form-item label="订单">{{ current?.orderTitle }}（{{ current?.orderNumber }}）</el-form-item>
        <el-form-item label="仲裁结果" prop="status">
          <el-select v-model="verdictForm.status" placeholder="请选择仲裁结果" style="width: 200px">
            <el-option label="退款用户" :value="1" />
            <el-option label="放款技能者" :value="2" />
            <el-option label="驳回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="仲裁结论" prop="verdict">
          <el-input v-model="verdictForm.verdict" type="textarea" :rows="3" placeholder="请填写仲裁结论说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verdictVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitVerdict">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { pageDisputes, verdictDispute } from '@/api'
import { DISPUTE_STATUS, DISPUTE_REASON_TYPE, fmtTime } from '@/utils/dict'

const query = reactive({ page: 1, pageSize: 10, status: null })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const formRef = ref()
const verdictVisible = ref(false)
const submitting = ref(false)
const current = ref(null)
const verdictForm = reactive({ status: null, verdict: '' })

const rules = {
  status: [{ required: true, message: '请选择仲裁结果', trigger: 'change' }],
  verdict: [{ required: true, message: '请填写仲裁结论', trigger: 'blur' }],
}

function parseEvidence(evidenceUrls) {
  if (!evidenceUrls) return []
  if (Array.isArray(evidenceUrls)) return evidenceUrls
  try {
    const parsed = JSON.parse(evidenceUrls)
    return Array.isArray(parsed) ? parsed : [evidenceUrls]
  } catch {
    return evidenceUrls.split(',').map((s) => s.trim()).filter(Boolean)
  }
}

async function load() {
  loading.value = true
  try {
    const data = await pageDisputes(query)
    list.value = data.list || data.records || []
    total.value = Number(data.total) || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  load()
}

function onReset() {
  Object.assign(query, { page: 1, status: null })
  load()
}

function openVerdict(row) {
  current.value = row
  Object.assign(verdictForm, { status: null, verdict: '' })
  verdictVisible.value = true
}

async function submitVerdict() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await verdictDispute(current.value.id, { status: verdictForm.status, verdict: verdictForm.verdict })
    ElMessage.success('仲裁完成')
    verdictVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
