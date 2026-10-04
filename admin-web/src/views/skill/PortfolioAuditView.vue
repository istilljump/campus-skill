<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="审核状态" clearable style="width: 140px">
        <el-option v-for="(v, k) in PORTFOLIO_STATUS" :key="k" :label="v.label" :value="Number(k)" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="skillerId" label="技能者ID" width="90" />
      <el-table-column prop="title" label="作品标题" min-width="150" show-overflow-tooltip />
      <el-table-column prop="categoryId" label="类目ID" width="80" />
      <el-table-column label="封面" width="90">
        <template #default="{ row }">
          <el-image v-if="row.coverUrl" :src="row.coverUrl" :preview-src-list="[row.coverUrl]"
            preview-teleported fit="cover" style="width: 40px; height: 40px; border-radius: 4px" />
          <span v-else class="muted">无</span>
        </template>
      </el-table-column>
      <el-table-column label="作品图" min-width="120">
        <template #default="{ row }">
          <template v-if="parseWorks(row.workUrls).length">
            <el-link v-for="(u, i) in parseWorks(row.workUrls)" :key="i" type="primary" :href="u" target="_blank"
              style="margin-right: 8px">图{{ i + 1 }}</el-link>
          </template>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="作品描述" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="PORTFOLIO_STATUS[row.status]?.type">{{ PORTFOLIO_STATUS[row.status]?.label || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditOpinion" label="审核意见" min-width="120" show-overflow-tooltip />
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" link type="primary" @click="openProcess(row)">审核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pager">
      <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.pageSize" :page-sizes="[10, 20, 50]"
        @change="load" />
    </div>

    <el-dialog v-model="processVisible" title="作品审核" width="460px">
      <el-form ref="formRef" :model="processForm" :rules="rules" label-width="80px">
        <el-form-item label="作品">{{ current?.title }}</el-form-item>
        <el-form-item label="审核结果" prop="status">
          <el-radio-group v-model="processForm.status">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="processForm.status === 1" label="技能等级" prop="skillLevel">
          <el-select v-model="processForm.skillLevel" placeholder="请选择技能等级" style="width: 160px">
            <el-option v-for="(v, k) in SKILL_LEVEL" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核意见" prop="auditOpinion">
          <el-input v-model="processForm.auditOpinion" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitProcess">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { pagePortfolioAudit, processPortfolioAudit } from '@/api'
import { PORTFOLIO_STATUS, SKILL_LEVEL, fmtTime } from '@/utils/dict'

const query = reactive({ page: 1, pageSize: 10, status: null })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const formRef = ref()
const processVisible = ref(false)
const submitting = ref(false)
const current = ref(null)
const processForm = reactive({ id: null, status: 1, skillLevel: null, auditOpinion: '' })

const rules = {
  status: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  skillLevel: [{ required: true, message: '通过时必须选择技能等级', trigger: 'change' }],
}

function parseWorks(workUrls) {
  if (!workUrls) return []
  if (Array.isArray(workUrls)) return workUrls
  try {
    const parsed = JSON.parse(workUrls)
    return Array.isArray(parsed) ? parsed : [workUrls]
  } catch {
    return workUrls.split(',').map((s) => s.trim()).filter(Boolean)
  }
}

async function load() {
  loading.value = true
  try {
    const data = await pagePortfolioAudit(query)
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

function openProcess(row) {
  current.value = row
  Object.assign(processForm, { id: row.id, status: 1, skillLevel: null, auditOpinion: '' })
  processVisible.value = true
}

async function submitProcess() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await processPortfolioAudit(processForm)
    ElMessage.success('审核完成')
    processVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
