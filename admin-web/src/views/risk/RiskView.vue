<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="处置状态" clearable style="width: 140px">
        <el-option label="未处置" :value="0" />
        <el-option label="已处置" :value="1" />
      </el-select>
      <el-select v-model="query.level" placeholder="风险等级" clearable style="width: 140px">
        <el-option v-for="(v, k) in RISK_LEVEL" :key="k" :label="v.label" :value="Number(k)" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="告警ID" width="80" />
      <el-table-column label="规则" width="150">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.ruleCode }}</el-tag>
          <span style="margin-left:6px">{{ row.ruleName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="等级" width="90">
        <template #default="{ row }">
          <el-tag :type="RISK_LEVEL[row.riskLevel]?.type" effect="dark" size="small">
            {{ RISK_LEVEL[row.riskLevel]?.label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="对象" width="130">
        <template #default="{ row }">
          {{ row.targetType === 2 ? '技能者' : '用户' }} #{{ row.targetId }}
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="命中详情" min-width="280" show-overflow-tooltip />
      <el-table-column label="命中时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
            {{ row.status === 1 ? '已处置' : '未处置' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" link type="primary" @click="openHandle(row)">处置</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无风控告警" :image-size="72" /></template>
    </el-table>

    <div class="table-pager">
      <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.pageSize" :page-sizes="[10, 20, 50]"
        @change="load" />
    </div>

    <el-dialog v-model="handleVisible" title="风控告警处置" width="460px">
      <el-descriptions :column="1" border size="small" style="margin-bottom:12px">
        <el-descriptions-item label="规则">{{ current?.ruleCode }} {{ current?.ruleName }}</el-descriptions-item>
        <el-descriptions-item label="详情">{{ current?.detail }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px">
        <el-form-item label="处置备注">
          <el-input v-model="handleForm.remark" type="textarea" :rows="3"
            placeholder="填写处置结论，如：已人工复核/已联系用户/确认误报" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">确认处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { pageRiskLogs, handleRiskLog } from '@/api'
import { fmtTime } from '@/utils/dict'

const RISK_LEVEL = {
  1: { label: '低', type: 'info' },
  2: { label: '中', type: 'warning' },
  3: { label: '高', type: 'danger' },
}

const query = reactive({ page: 1, pageSize: 10, status: null, level: null })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const handleVisible = ref(false)
const submitting = ref(false)
const current = ref(null)
const handleForm = reactive({ id: null, remark: '' })

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.status !== null && query.status !== '') params.status = query.status
    if (query.level !== null && query.level !== '') params.level = query.level
    const data = await pageRiskLogs(params)
    list.value = data?.records || []
    total.value = Number(data?.total) || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  load()
}

function onReset() {
  query.status = null
  query.level = null
  query.page = 1
  load()
}

function openHandle(row) {
  current.value = row
  handleForm.id = row.id
  handleForm.remark = ''
  handleVisible.value = true
}

async function submitHandle() {
  submitting.value = true
  try {
    await handleRiskLog({ id: handleForm.id, remark: handleForm.remark || null })
    ElMessage.success('处置完成')
    handleVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
