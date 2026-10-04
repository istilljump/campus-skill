<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="审核状态" clearable style="width: 140px">
        <el-option v-for="(v, k) in AUDIT_RECORD_STATUS" :key="k" :label="v.label" :value="Number(k)" />
      </el-select>
      <el-input v-model="query.studentNo" placeholder="学号" clearable />
      <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="申请ID" width="80" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="studentNo" label="学号" width="140" />
      <el-table-column prop="campus" label="校区" width="110" />
      <el-table-column prop="college" label="学院" min-width="130" />
      <el-table-column prop="idCard" label="身份证号" width="180" />
      <el-table-column label="学生证" width="90">
        <template #default="{ row }">
          <el-image v-if="row.studentCardImg" :src="row.studentCardImg" :preview-src-list="[row.studentCardImg]"
            preview-teleported fit="cover" style="width: 40px; height: 40px; border-radius: 4px" />
          <span v-else class="muted">未上传</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="AUDIT_RECORD_STATUS[row.status]?.type">{{ AUDIT_RECORD_STATUS[row.status]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.applyTime) }}</template>
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

    <el-dialog v-model="processVisible" title="技能者认证审核" width="440px">
      <el-form label-width="80px">
        <el-form-item label="申请人">{{ current?.realName }}（{{ current?.studentNo }}）</el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="processForm.status">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="processForm.auditRemark" type="textarea" :rows="3" placeholder="选填" />
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
import { pageAudits, processAudit } from '@/api'
import { AUDIT_RECORD_STATUS, fmtTime } from '@/utils/dict'

const query = reactive({ page: 1, pageSize: 10, status: null, studentNo: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const processVisible = ref(false)
const submitting = ref(false)
const current = ref(null)
const processForm = reactive({ id: null, status: 1, auditRemark: '' })

async function load() {
  loading.value = true
  try {
    const data = await pageAudits(query)
    list.value = data.records || []
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
  Object.assign(query, { page: 1, status: null, studentNo: '' })
  load()
}

function openProcess(row) {
  current.value = row
  Object.assign(processForm, { id: row.id, status: 1, auditRemark: '' })
  processVisible.value = true
}

async function submitProcess() {
  submitting.value = true
  try {
    await processAudit(processForm)
    ElMessage.success('审核完成')
    processVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
