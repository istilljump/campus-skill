<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="提现状态" clearable style="width: 140px">
        <el-option v-for="(v, k) in WITHDRAW_STATUS" :key="k" :label="v.label" :value="Number(k)" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="申请ID" width="80" />
      <el-table-column label="提现金额" width="120">
        <template #default="{ row }"><span class="money">{{ fmtMoney(row.amount) }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="WITHDRAW_STATUS[row.status]?.type">{{ WITHDRAW_STATUS[row.status]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.applyTime) }}</template>
      </el-table-column>
      <el-table-column label="打款时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.payTime) }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" link type="primary" @click="openProcess(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pager">
      <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.pageSize" :page-sizes="[10, 20, 50]"
        @change="load" />
    </div>

    <el-dialog v-model="processVisible" title="提现处理" width="440px">
      <el-form label-width="80px">
        <el-form-item label="提现金额">
          <span class="money">{{ fmtMoney(current?.amount) }}</span>
        </el-form-item>
        <el-form-item label="处理结果">
          <el-radio-group v-model="processForm.status">
            <el-radio :value="1">打款</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="processForm.remark" type="textarea" :rows="3" placeholder="选填" />
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
import { pageWithdraws, processWithdraw } from '@/api'
import { WITHDRAW_STATUS, fmtTime, fmtMoney } from '@/utils/dict'

const query = reactive({ page: 1, pageSize: 10, status: null })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const processVisible = ref(false)
const submitting = ref(false)
const current = ref(null)
const processForm = reactive({ id: null, status: 1, remark: '' })

async function load() {
  loading.value = true
  try {
    const data = await pageWithdraws(query)
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
  Object.assign(query, { page: 1, status: null })
  load()
}

function openProcess(row) {
  current.value = row
  Object.assign(processForm, { id: row.id, status: 1, remark: '' })
  processVisible.value = true
}

async function submitProcess() {
  submitting.value = true
  try {
    await processWithdraw(processForm)
    ElMessage.success('处理完成')
    processVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
