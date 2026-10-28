<template>
  <div class="card-page" v-loading="loading">
    <template v-if="card">
      <div class="poster-wrap">
        <canvas ref="canvasRef" class="poster"></canvas>
      </div>
      <div class="actions">
        <el-button type="primary" size="large" @click="download">下载海报 PNG</el-button>
        <el-button size="large" @click="goHome">逛逛技能市场</el-button>
      </div>
      <p class="tip">扫码或转发名片页即可查看该技能者的公开信息（不含手机号等隐私）</p>
    </template>
    <div v-else-if="!loading" class="empty">
      <p>名片不存在或技能者已注销</p>
      <el-button type="primary" @click="goHome">去 CampusSkill 首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { getSkillerCard } from '@/api'
import { LEVEL_NAMES } from '@/utils/dict'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const card = ref(null)
const canvasRef = ref(null)

const LEVEL_COLORS = { 1: '#9499a0', 2: '#b87333', 3: '#8c9bab', 4: '#d4a017' }

function cardUrl(id) {
  // 与当前部署路径同源：可直接挂 /web-app/ 或根路径下
  return `${location.origin}${location.pathname}#/card/${id}`
}

function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

async function drawPoster() {
  const c = card.value
  const canvas = canvasRef.value
  if (!c || !canvas) return
  const W = 750; const H = 1060
  const dpr = 2
  canvas.width = W * dpr
  canvas.height = H * dpr
  const ctx = canvas.getContext('2d')
  ctx.scale(dpr, dpr)

  // 背景：蓝白渐变 + 两个柔光斑
  const bg = ctx.createLinearGradient(0, 0, 0, H)
  bg.addColorStop(0, '#e5f7fd')
  bg.addColorStop(0.55, '#f7fbff')
  bg.addColorStop(1, '#fff6fa')
  ctx.fillStyle = bg
  ctx.fillRect(0, 0, W, H)
  const glow1 = ctx.createRadialGradient(90, 130, 10, 90, 130, 260)
  glow1.addColorStop(0, 'rgba(0,174,236,.18)')
  glow1.addColorStop(1, 'rgba(0,174,236,0)')
  ctx.fillStyle = glow1
  ctx.fillRect(0, 0, W, 400)
  const glow2 = ctx.createRadialGradient(680, 950, 10, 680, 950, 280)
  glow2.addColorStop(0, 'rgba(251,114,153,.14)')
  glow2.addColorStop(1, 'rgba(251,114,153,0)')
  ctx.fillStyle = glow2
  ctx.fillRect(0, 620, W, 440)

  // 头像：渐变圆角块 + 展示名首字
  const ax = 60; const ay = 80; const as = 108
  const av = ctx.createLinearGradient(ax, ay, ax + as, ay + as)
  av.addColorStop(0, '#00aeec')
  av.addColorStop(1, '#0ea5e9')
  ctx.fillStyle = av
  roundRect(ctx, ax, ay, as, as, 26)
  ctx.fill()
  ctx.fillStyle = '#ffffff'
  ctx.font = 'bold 52px "PingFang SC", "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText((c.name || '技').slice(0, 1), ax + as / 2, ay + as / 2 + 4)

  // 名字 + 等级徽章
  ctx.textAlign = 'left'
  ctx.fillStyle = '#18191c'
  ctx.font = 'bold 40px "PingFang SC", "Microsoft YaHei", sans-serif'
  ctx.fillText(c.name || '技能者', ax, ay + as + 62)
  const level = LEVEL_NAMES[c.runnerLevel] || '普通'
  ctx.font = '600 22px "PingFang SC", "Microsoft YaHei", sans-serif'
  const tagW = ctx.measureText(level).width + 36
  ctx.fillStyle = LEVEL_COLORS[c.runnerLevel] || '#9499a0'
  roundRect(ctx, ax + ctx.measureText(c.name || '技能者').width * 2.05 + 14, ay + as + 34, tagW, 40, 20)
  ctx.fill()
  ctx.fillStyle = '#ffffff'
  ctx.fillText(level, ax + ctx.measureText(c.name || '技能者').width * 2.05 + 18 + 18, ay + as + 55)

  // 数据三列：评分 / 信用 / 完成单
  const stats = [
    { label: '评分', value: Number(c.score ?? 5).toFixed(1) },
    { label: '信用分', value: String(c.creditScore ?? 100) },
    { label: '完成订单', value: String(c.completedOrders ?? 0) },
  ]
  const sy = ay + as + 116
  const colW = (W - 120 - 40) / 3
  stats.forEach((s, i) => {
    const x = 60 + i * (colW + 20)
    ctx.fillStyle = '#ffffff'
    roundRect(ctx, x, sy, colW, 108, 16)
    ctx.fill()
    ctx.fillStyle = '#18191c'
    ctx.font = 'bold 38px "PingFang SC", "Microsoft YaHei", sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(s.value, x + colW / 2, sy + 44)
    ctx.fillStyle = '#9499a0'
    ctx.font = '20px "PingFang SC", "Microsoft YaHei", sans-serif'
    ctx.fillText(s.label, x + colW / 2, sy + 82)
  })

  // 在售服务 Top3
  ctx.textAlign = 'left'
  ctx.fillStyle = '#61666d'
  ctx.font = '600 26px "PingFang SC", "Microsoft YaHei", sans-serif'
  ctx.fillText('在售服务', 60, sy + 172)
  const rows = (c.services || []).slice(0, 3)
  rows.forEach((s, i) => {
    const y = sy + 196 + i * 92
    ctx.fillStyle = '#ffffff'
    roundRect(ctx, 60, y, W - 120, 78, 14)
    ctx.fill()
    ctx.fillStyle = '#18191c'
    ctx.font = '24px "PingFang SC", "Microsoft YaHei", sans-serif'
    const title = s.title.length > 14 ? `${s.title.slice(0, 14)}…` : s.title
    ctx.fillText(title, 84, y + 48)
    ctx.fillStyle = '#fb7299'
    ctx.font = 'bold 26px "PingFang SC", "Microsoft YaHei", sans-serif'
    ctx.textAlign = 'right'
    ctx.fillText(`¥${Number(s.price).toFixed(0)}`, W - 84 - 64, y + 48)
    ctx.fillStyle = '#c9cdd4'
    ctx.font = '18px "PingFang SC", "Microsoft YaHei", sans-serif'
    ctx.fillText(`已售 ${s.salesCount ?? 0}`, W - 72, y + 48)
    ctx.textAlign = 'left'
  })

  // 二维码（本地生成，不依赖外网）
  const qrData = await QRCode.toDataURL(cardUrl(c.runnerId), { width: 400, margin: 1, color: { dark: '#18191c', light: '#ffffff' } })
  const qr = new Image()
  await new Promise((resolve) => { qr.onload = resolve; qr.src = qrData })
  const qs = 190; const qx = (W - qs) / 2; const qy = H - 300
  ctx.fillStyle = '#ffffff'
  roundRect(ctx, qx - 16, qy - 16, qs + 32, qs + 32, 18)
  ctx.fill()
  ctx.drawImage(qr, qx, qy, qs, qs)
  ctx.textAlign = 'center'
  ctx.fillStyle = '#61666d'
  ctx.font = '22px "PingFang SC", "Microsoft YaHei", sans-serif'
  ctx.fillText('扫码查看技能者名片', W / 2, qy + qs + 42)

  // 品牌脚注
  ctx.fillStyle = '#00aeec'
  ctx.font = 'bold 24px "PingFang SC", "Microsoft YaHei", sans-serif'
  ctx.fillText('CampusSkill 校园技能工坊', W / 2, H - 36)
}

async function download() {
  const canvas = canvasRef.value
  if (!canvas) return
  const a = document.createElement('a')
  a.href = canvas.toDataURL('image/png')
  a.download = `CampusSkill-技能者名片-${card.value?.name || card.value?.runnerId}.png`
  a.click()
}

function goHome() {
  router.push('/home')
}

onMounted(async () => {
  try {
    card.value = await getSkillerCard(route.params.runnerId)
  } catch (e) {
    card.value = null
  } finally {
    loading.value = false
  }
  await nextTick()
  drawPoster()
})
</script>

<style scoped>
.card-page {
  min-height: 100vh;
  background: linear-gradient(160deg, #f0faff, #f7f9ff 45%, #fff6fa);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 16px;
  gap: 18px;
}
.poster-wrap {
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 174, 236, 0.18);
  overflow: hidden;
  line-height: 0;
}
.poster {
  width: 340px;
  height: auto;
}
.actions {
  display: flex;
  gap: 12px;
}
.tip {
  color: #9499a0;
  font-size: 12px;
  margin: 0;
  text-align: center;
}
.empty {
  margin-top: 20vh;
  text-align: center;
  color: #61666d;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
