<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-input-number v-model="query.limit" :min="1" :max="100" controls-position="right" style="width: 140px" />
      <el-button type="primary" :icon="Search" @click="load">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column label="排名" width="80">
        <template #default="{ $index }">
          <el-tag v-if="$index < 3" type="warning" effect="dark">{{ $index + 1 }}</el-tag>
          <span v-else>{{ $index + 1 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="skillerId" label="技能者ID" width="90" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column label="信用分" width="100">
        <template #default="{ row }">
          <el-tag :type="creditType(row.creditScore)">{{ row.creditScore }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="技能等级" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.skillLevel" type="warning" effect="plain">{{ SKILL_LEVEL[row.skillLevel] || row.skillLevel }}</el-tag>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="score" label="评分" width="80" />
      <el-table-column prop="completedOrders" label="完成单数" width="100" />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { creditRank } from '@/api'
import { SKILL_LEVEL } from '@/utils/dict'

const query = reactive({ limit: 10 })
const list = ref([])
const loading = ref(false)

function creditType(score) {
  if (score >= 90) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

async function load() {
  loading.value = true
  try {
    list.value = (await creditRank(query.limit)) || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
