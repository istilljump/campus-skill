// 演示就绪校验：以「测试用户」双身份登录，逐板块拉取接口，确认答辩演示时每个板块都有内容。
// 用法：node tools/verify-demo.mjs   （需先启动应用）
const BASE = process.env.BASE || 'http://localhost:8080'

async function api(method, url, body, token, headerName = 'authentication') {
  const headers = {}
  if (body) headers['Content-Type'] = 'application/json'
  if (token) headers[headerName] = token
  const r = await fetch(BASE + url, { method, headers, body: body ? JSON.stringify(body) : undefined })
  try { return await r.json() } catch (e) { return { code: 0, msg: `HTTP ${r.status} 非 JSON 响应` } }
}

let pass = 0, fail = 0
function check(label, ok, detail) {
  ok ? pass++ : fail++
  const mark = ok ? '✅' : '❌'
  console.log(`${mark} ${label}${detail !== undefined ? '  → ' + detail : ''}`)
}

const DEMO = { username: process.env.DEMO_USER || 'demo1', password: process.env.DEMO_PASS || '123456' }

console.log('=== 1. 登录（用户端 / 技能者端 双身份）===')
const uLogin = await api('POST', '/user/user/accountLogin', DEMO)
check('用户端账号登录', uLogin.code === 1, uLogin.msg || DEMO.username)
const sLogin = await api('POST', '/skiller/auth/accountLogin', DEMO)
check('技能者端账号登录', sLogin.code === 1, sLogin.msg || DEMO.username)
if (uLogin.code !== 1 || sLogin.code !== 1) { console.log('\n登录失败，后续校验跳过。'); process.exit(1) }
const UT = uLogin.data.token, ST = sLogin.data.token

console.log('\n=== 2. 用户端板块 ===')
const profile = await api('GET', '/user/user/profile', null, UT)
check('我的资料', profile.code === 1 && !!profile.data?.name,
  `${profile.data?.name} · 信用分 ${profile.data?.creditScore} · 发单 ${profile.data?.publishOrderCount}`)

const hall = await api('GET', '/user/hall/list?page=1&pageSize=10', null, UT)
check('悬赏大厅（待接单）', (hall.data?.records || []).length > 0,
  `${hall.data?.total} 条待接单，首条：${hall.data?.records?.[0]?.title}`)

const allOrders = await api('GET', '/user/order/page?page=1&pageSize=50', null, UT)
const byStatus = {}
for (const o of allOrders.data?.records || []) byStatus[o.status] = (byStatus[o.status] || 0) + 1
check('我的订单（各状态覆盖）', Object.keys(byStatus).length >= 4,
  Object.entries(byStatus).map(([k, v]) => `状态${k}:${v}`).join(' '))

const detailId = (allOrders.data?.records || []).find(o => o.status === 3 || o.status === 4)?.id
if (detailId) {
  const d = await api('GET', '/user/order/detail/' + detailId, null, UT)
  check('订单详情（含倒计时字段）', d.code === 1 && !!d.data?.timeoutTime,
    `${d.data?.title} · 超时时间 ${d.data?.timeoutTime || '—'}`)
} else {
  check('订单详情（含倒计时字段）', false, '未找到进行中/已交付订单')
}

const market = await api('GET', '/user/market?page=1&pageSize=10', null, UT)
check('技能市场', (market.data?.records || []).length > 0, `${market.data?.total} 项上架服务`)

const wallet = await api('GET', '/user/wallet/balance', null, UT)
const tx = await api('GET', '/user/wallet/transactions?page=1&pageSize=10', null, UT)
check('我的钱包 + 收支明细', wallet.code === 1 && (tx.data?.records || []).length > 0,
  `余额 ￥${wallet.data?.balance} · 流水 ${tx.data?.total} 条`)

const msgs = await api('GET', '/user/message/list', null, UT)
const unread = await api('GET', '/user/message/unread', null, UT)
check('消息中心', (msgs.data || []).length > 0, `消息 ${(msgs.data || []).length} 条 · 未读 ${unread.data}`)

const addr = await api('GET', '/user/addressBook/list', null, UT)
check('地址管理', (addr.data || []).length > 0, `${(addr.data || []).length} 个地址`)

const uBooking = await api('GET', '/user/booking/page?page=1&pageSize=10', null, UT)
check('我的预约', (uBooking.data?.records || []).length > 0, `${uBooking.data?.total} 条预约`)

console.log('\n=== 3. 技能者工作台板块（测试用户）===')
const center = await api('GET', '/skiller/info/center', null, ST)
const c = center.data || {}
check('技能者中心（数据总览）', center.code === 1,
  `${c.name} · 等级 ${c.runnerLevel} · 今日已接 ${c.todayOrderCount}/${c.dailyOrderLimit} · 今日收入 ￥${c.todayIncome} · 余额 ￥${c.balance} · 评分 ${c.score} · 信用 ${c.creditScore} · 作品定级 C${c.skillLevel}`)
check('今日收入 > 0（可演示）', Number(c.todayIncome) > 0, `￥${c.todayIncome}`)

const trend = await api('GET', '/skiller/info/trend?days=7', null, ST)
const trendDays = (trend.data || []).filter(p => Number(p.amount) > 0).length
check('近 7 日收入趋势', trendDays > 0, `${trendDays} 天有收入数据`)

const skHall = await api('GET', '/skiller/orders/hall?page=1&pageSize=10', null, ST)
check('悬赏大厅（技能者抢单）', (skHall.data?.records || []).length > 0, `${skHall.data?.total} 条可抢`)

const skOrders = await api('GET', '/skiller/orders/page?page=1&pageSize=50', null, ST)
const skBy = {}
for (const o of skOrders.data?.records || []) skBy[o.status] = (skBy[o.status] || 0) + 1
check('我的接单（各状态覆盖）', Object.keys(skBy).length >= 5,
  Object.entries(skBy).map(([k, v]) => `状态${k}:${v}`).join(' '))

const services = await api('GET', '/skiller/service/list', null, ST)
check('我的服务', (services.data || []).length > 0,
  `${(services.data || []).length} 项（上架 ${(services.data || []).filter(s => s.status === 1).length} / 下架 ${(services.data || []).filter(s => s.status === 0).length}）`)

const pf = await api('GET', '/skiller/portfolio', null, ST)
const pfList = Array.isArray(pf.data) ? pf.data : (pf.data?.records || [])
check('我的作品（含待审/驳回异常态）', pfList.length > 0,
  `${pfList.length} 件（待审 ${pfList.filter(p => p.status === 0).length} / 通过 ${pfList.filter(p => p.status === 1).length} / 驳回 ${pfList.filter(p => p.status === 2).length}）`)

const skBook = await api('GET', '/skiller/booking/page?page=1&pageSize=10', null, ST)
check('预约单（含各状态）', (skBook.data?.records || []).length > 0,
  (skBook.data?.records || []).map(b => `状态${b.status}`).join(' '))

const wd = await api('GET', '/skiller/wallet/withdraw/page?page=1&pageSize=10', null, ST)
check('提现记录（含待处理/已打款/已驳回）', (wd.data?.records || []).length > 0,
  (wd.data?.records || []).map(r => `¥${r.amount}(状态${r.status})`).join(' '))

const skTx = await api('GET', '/skiller/wallet/transactions?page=1&pageSize=10', null, ST)
check('技能者收支明细', (skTx.data?.records || []).length > 0, `${skTx.data?.total} 条`)

const skMsg = await api('GET', '/skiller/message/list', null, ST)
const skUnread = await api('GET', '/skiller/message/unread', null, ST)
check('技能者消息中心', (skMsg.data || []).length > 0, `消息 ${(skMsg.data || []).length} 条 · 未读 ${skUnread.data}`)

console.log('\n=== 4. 管理端 + 静态资源（图标）===')
const aLogin = await api('POST', '/admin/employee/login', { username: 'admin', password: '123456' }, null, 'token')
check('管理端登录', aLogin.code === 1, aLogin.msg || 'admin')
if (aLogin.code === 1) {
  const AT = aLogin.data.token
  const ov = await api('GET', '/admin/statistics/overview', null, AT, 'token')
  check('数据大屏总览', ov.code === 1, JSON.stringify(ov.data))
  const tr = await api('GET', '/admin/statistics/trend?days=7', null, AT, 'token')
  check('成交趋势', (tr.data || []).length > 0, `${(tr.data || []).length} 天`)
  const disp = await api('GET', '/admin/dispute/page?page=1&pageSize=10', null, AT, 'token')
  check('仲裁工单', (disp.data?.records || []).length > 0, `${disp.data?.total} 条`)
}

for (const [path, type] of [['/favicon.svg', 'image/svg+xml'], ['/favicon.ico', 'image'], ['/manifest.webmanifest', 'json'], ['/icon-192.png', 'image'], ['/apple-touch-icon.png', 'image']]) {
  const r = await fetch(BASE + path)
  check(`静态资源 ${path}`, r.ok, `HTTP ${r.status} · ${r.headers.get('content-type')}`)
}
const cov = await fetch(BASE + '/assets/covers/poster.svg')
check('本地封面占位图', cov.ok, `HTTP ${cov.status}`)

console.log(`\n===== 校验完成：通过 ${pass} 项，失败 ${fail} 项 =====`)
process.exit(fail === 0 ? 0 : 1)
