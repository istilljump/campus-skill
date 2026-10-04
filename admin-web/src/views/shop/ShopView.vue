<template>
  <div class="page-card shop-card">
    <h3>营业状态</h3>
    <p class="muted">打烊后用户端将无法发单、技能者端将无法接单。</p>
    <div class="shop-status">
      <el-icon :size="60" :color="shopStatus === 1 ? '#16a34a' : '#9ca3af'">
        <component :is="shopStatus === 1 ? 'CircleCheckFilled' : 'RemoveFilled'" />
      </el-icon>
      <div>
        <div class="shop-text">{{ shopStatus === 1 ? '营业中' : '已打烊' }}</div>
        <el-switch v-model="shopOn" size="large" :loading="switching" active-text="营业" inactive-text="打烊"
          @change="onToggle" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getShopStatus, setShopStatus } from '@/api'

const shopStatus = ref(1)
const shopOn = ref(true)
const switching = ref(false)

async function load() {
  shopStatus.value = await getShopStatus()
  shopOn.value = shopStatus.value === 1
}

async function onToggle() {
  switching.value = true
  try {
    const target = shopOn.value ? 1 : 0
    await setShopStatus(target)
    shopStatus.value = target
    ElMessage.success(target === 1 ? '已开启营业' : '已打烊')
  } catch {
    shopOn.value = shopStatus.value === 1
  } finally {
    switching.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.shop-card {
  max-width: 560px;
}

.shop-status {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 20px;
}

.shop-text {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}
</style>
