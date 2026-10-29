// CampusSkill 负载压测 runner（v11 可信交易保障体系）
//
// 场景：
//   hall  —— 大厅/货架列表读路径 QPS 与分位数（配合 campus.cache.enabled 开关做缓存 A/B）
//   grab  —— 抢单竞态：S 名技能者并发抢同一单，统计成功率与延迟（配合 --campus.grab.legacy=true 做优化 A/B）
//   idem  —— 幂等重复支付：N 线程同键并发，验证"恰好成功一次 + 账实相符"
//
// 前置：应用运行于 localhost:8080；压测数据用数据工厂预置：
//   mysql --default-character-set=utf8mb4 campus_runner < sql/data_factory.sql
//   CALL proc_factory_skillers('BENCH', 60);   -- 技能者 code = lfBENCHu{i}
//   CALL proc_factory_orders('BENCH', 50);     -- 已支付待接单悬赏
//   CALL proc_factory_users('BENCH', 60);
// 清理：sql/loadtest_reset.sql
//
// 用法：
//   node bench.mjs hall --users 50 --duration 20
//   node bench.mjs grab --groups 10 --skillers 30
//   node bench.mjs idem --threads 20
//   node bench.mjs all          # 快速三场景串跑（演示/巡检用）

const BASE = process.env.BASE_URL || 'http://localhost:8080';

// ---------- 工具 ----------
async function api(method, url, body, token, headers = {}) {
  const started = performance.now();
  let ok = false, status = 0;
  try {
    const res = await fetch(BASE + url, {
      method,
      headers: { 'Content-Type': 'application/json', ...(token ? { authentication: token } : {}), ...headers },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    status = res.status;
    const j = await res.json().catch(() => null);
    ok = j?.code === 1;
    return { ok, status, body: j, ms: performance.now() - started };
  } catch (e) {
    return { ok, status, body: null, ms: performance.now() - started, error: String(e) };
  }
}

const sleep = (ms) => new Promise(r => setTimeout(r, ms));

function pct(sorted, p) {
  if (!sorted.length) return 0;
  const idx = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1);
  return sorted[Math.max(0, idx)];
}

function report(name, samples, extra = {}, wallMs = null) {
  const times = samples.map(s => s.ms).sort((a, b) => a - b);
  const okCount = samples.filter(s => s.ok).length;
  const elapsedSec = wallMs ? wallMs / 1000 : 0;
  const row = {
    场景: name,
    请求数: samples.length,
    成功: okCount,
    失败: samples.length - okCount,
    QPS: elapsedSec > 0 ? +(samples.length / elapsedSec).toFixed(1) : null,
    'P50(ms)': +pct(times, 50).toFixed(1),
    'P95(ms)': +pct(times, 95).toFixed(1),
    'P99(ms)': +pct(times, 99).toFixed(1),
    ...extra,
  };
  console.log(JSON.stringify(row));
  return row;
}

async function loginAs(code) {
  const r = await api('POST', '/user/user/login', { code });
  if (!r.ok || !r.body?.data?.token) throw new Error(`登录失败 code=${code}: ${JSON.stringify(r.body).slice(0, 200)}`);
  return r.body.data.token;
}

async function loginSkillerAs(code) {
  const r = await api('POST', '/skiller/auth/login', { code });
  if (!r.ok || !r.body?.data?.token) throw new Error(`技能者登录失败 code=${code}: ${JSON.stringify(r.body).slice(0, 200)}`);
  return r.body.data.token;
}

// ---------- 场景 1：读路径压测（缓存 A/B） ----------
// path 可选 /user/hall/list（未缓存对照组）或 /user/market（货架缓存实验组）
async function benchRead({ users = 30, duration = 15, rampup = 3, path = '/user/market?page=1&pageSize=10&sort=default' }) {
  const name = path.includes('market') ? '货架列表(缓存组)' : '大厅列表(对照组)';
  console.log(`== 场景 ${name}：${users} 并发 × ${duration}s（预热 ${rampup}s）==`);
  const tokens = [];
  for (let i = 1; i <= users; i++) tokens.push(await loginAs(`lfBENCHu${i}`));

  const samples = [];
  const deadline = Date.now() + rampup * 1000; // 预热期样本不计入
  let warm = true;
  const stopAt = Date.now() + (rampup + duration) * 1000;

  async function worker(token) {
    while (Date.now() < stopAt) {
      if (warm && Date.now() > deadline) warm = false;
      const s = await api('GET', path, undefined, token);
      if (!warm) samples.push(s);
    }
  }
  const wallStart = Date.now();
  await Promise.all(tokens.map(worker));
  const wallMs = Date.now() - wallStart - rampup * 1000; // 预热期不计入
  return report(name, samples, { 路径: path }, wallMs);
}

// ---------- 场景 2：抢单竞态（优化 A/B） ----------
// gap：组间间隔（ms）。服务端对每个技能者有 10 秒 5 次的抢单限流，
// 连续压测需间隔 >10s 或把 skillers 池放大，否则限流会把整组拦成 0 中标（压测口径需规避）。
async function benchGrab({ groups = 5, skillers = 30, gap = 11000 }) {
  console.log(`== 场景 grab：${groups} 组 × ${skillers} 技能者并发抢同一单（组间 ${gap}ms）==`);
  const skTokens = [];
  for (let i = 1; i <= skillers; i++) skTokens.push(await loginSkillerAs(`lfBENCHu${i}`));

  // 取待接单订单（数据工厂造的 status=2）
  const publisher = await loginAs('lfBENCHu1');
  const hall = await api('GET', '/user/order/page?page=1&pageSize=100&status=2', undefined, publisher);
  const orders = (hall.body?.data?.records || []).slice(0, groups);
  if (orders.length < groups) throw new Error(`待接单订单不足：需要 ${groups}，仅 ${orders.length}（先跑数据工厂）`);

  const samples = [];
  let winners = 0;
  for (let gi = 0; gi < orders.length; gi++) {
    const o = orders[gi];
    const latch = new Promise(r => setTimeout(r, 50)); // 简单起搏
    const results = await Promise.all(skTokens.map(async (tk, idx) => {
      await latch;
      const s = await api('POST', '/skiller/orders/grab', { id: o.id }, tk);
      return s;
    }));
    const okCount = results.filter(r => r.ok).length;
    if (okCount === 1) winners++;
    results.forEach(r => samples.push(r));
    if (gi < orders.length - 1) await sleep(gap); // 规避限流窗口
  }
  return report('抢单竞态', samples, { 组数: groups, '每组并发': skillers, '恰好一人成功组数': winners });
}

// ---------- 场景 3：幂等重复支付 ----------
async function benchIdem({ threads = 20, rounds = 3 }) {
  console.log(`== 场景 idem：${threads} 线程同键并发充值 × ${rounds} 轮 ==`);
  const token = await loginAs('lfBENCHu2');
  const before = await api('GET', '/user/wallet/balance', undefined, token);
  const balance0 = Number(before.body?.data?.balance ?? 0);

  const samples = [];
  let allPassed = true;
  for (let round = 1; round <= rounds; round++) {
    const key = `bench-idem-${Date.now()}-${round}`;
    const results = await Promise.all(Array.from({ length: threads }, () =>
      api('POST', '/user/wallet/recharge', { amount: 10 }, token, { 'X-Idempotency-Key': key })));
    const okCount = results.filter(r => r.ok).length;
    results.forEach(r => samples.push(r));
    if (okCount !== 1) allPassed = false;
    await sleep(100);
  }
  const after = await api('GET', '/user/wallet/balance', undefined, token);
  const balance1 = Number(after.body?.data?.balance ?? 0);
  const expectedAdd = rounds * 10; // 每轮恰好一次成功
  const consistent = balance1 - balance0 === expectedAdd;
  if (!consistent) allPassed = false;
  return report('幂等重复支付', samples,
    { 线程: threads, 轮数: rounds, 余额增量: balance1 - balance0, 应增: expectedAdd, 幂等正确: allPassed });
}

// ---------- 入口 ----------
const [, , scenario, ...rest] = process.argv;
const args = {};
for (let i = 0; i < rest.length; i += 2) args[rest[i].replace(/^--/, '')] = rest[i + 1];

const n = (k, d) => (args[k] ? Number(args[k]) : d);
try {
  if (scenario === 'hall' || scenario === 'all') await benchRead({ users: n('users', 30), duration: n('duration', 15), rampup: n('rampup', 3), path: '/user/hall/list?page=1&pageSize=10' });
  if (scenario === 'market') await benchRead({ users: n('users', 30), duration: n('duration', 15), rampup: n('rampup', 3), path: n('path', '/user/market?page=1&pageSize=10&sort=default') });
  if (scenario === 'grab' || scenario === 'all') await benchGrab({ groups: n('groups', 5), skillers: n('skillers', 30), gap: n('gap', 11000) });
  if (scenario === 'idem' || scenario === 'all') await benchIdem({ threads: n('threads', 20), rounds: n('rounds', 3) });
  if (!scenario || scenario === 'all') console.log('== 压测完成 ==');
  if (!['hall', 'market', 'grab', 'idem', 'all'].includes(scenario)) {
    console.log('用法: node bench.mjs [hall|market|grab|idem|all] --users 50 --duration 20 --skillers 30 --groups 5 --threads 20');
  }
} catch (e) {
  console.error('压测失败:', e.message);
  process.exit(1);
}
