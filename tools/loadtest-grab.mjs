// CampusSkill 抢单接口压测（零依赖，Node 18+）
// 场景：N 个已认证技能者在同一时刻并发抢同一笔悬赏订单，重复多轮。
// 断言核心正确性：每轮恰好 1 人抢单成功（不双抢、不丢单）。
// 用法：node tools/loadtest-grab.mjs --concurrency 50 --rounds 5 --label baseline
// 结果写入 tools/loadtest/<label>.json，供 report 对比。
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const BASE = process.env.BASE_URL || 'http://localhost:8080';
const args = process.argv.slice(2);
const arg = (k, d) => { const i = args.indexOf('--' + k); return i >= 0 ? Number(args[i + 1]) : d; };
const CONCURRENCY = arg('concurrency', 50);
const ROUNDS = arg('rounds', 5);
const WARMUP = arg('warmup', 1);
const LABEL = args[args.indexOf('--label') + 1] || `run-${Date.now()}`;

const L = (r) => r?.data?.records ?? r?.data?.list ?? [];

async function api(method, url, body, token, headerName = 'authentication') {
  const t0 = performance.now();
  const res = await fetch(BASE + url, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { [headerName]: token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const j = await res.json().catch(() => ({}));
  return { j, ms: performance.now() - t0 };
}

async function main() {
  console.log(`== 压测准备：创建 ${CONCURRENCY} 个认证技能者 ==`);
  const admin = (await api('POST', '/admin/employee/login', { username: 'admin', password: '123456' }, undefined, 'token')).j.data.token;

  const skillers = [];
  const tag = `lt${Date.now().toString(36).slice(-6)}`;
  for (let i = 0; i < CONCURRENCY; i++) {
    const code = `${tag}_${i}`;
    //认证前走用户态登录（/skiller/auth/login 仅认证通过后可登录）
    const userTok = (await api('POST', '/user/user/login', { code })).j.data.token;
    const apply = await api('POST', '/user/skiller/apply', {
      realName: `压测技能者${i}`, studentNo: `LT${Date.now().toString().slice(-8)}${String(i).padStart(3, '0')}`,
      campus: '主校区', college: '计算机学院', idCard: `110101200001${String(i).padStart(6, '0')}`,
      studentCardImg: 'http://fake/img.jpg',
    }, userTok);
    if (!(apply.j?.msg || '').includes('审核中') && apply.j?.code !== 1) throw new Error('入驻失败 ' + JSON.stringify(apply.j));
    //管理员审核最新一条待审核记录
    const page = (await api('GET', '/admin/skillerAudit/page?page=1&pageSize=5&status=0', undefined, admin, 'token')).j;
    const rec = L(page)[0];
    if (rec) await api('PUT', '/admin/skillerAudit/process', { id: rec.id, status: 1, auditRemark: '压测' }, admin, 'token');
    const sk = (await api('POST', '/skiller/auth/login', { code })).j;
    if (!sk?.data?.token) throw new Error(`技能者${i}登录失败: ` + JSON.stringify(sk));
    skillers.push(sk.data.token);
    if ((i + 1) % 10 === 0) console.log(`  已就绪 ${i + 1}/${CONCURRENCY}`);
  }

  const user = (await api('POST', '/user/user/login', { code: 'lt_user_' + tag })).j.data.token;
  await api('POST', '/user/wallet/recharge', { amount: 1000 * ROUNDS + 500 }, user);

  async function oneRound(roundNo) {
    //发布并支付一单悬赏
    const sub = (await api('POST', '/user/order/submit', {
      typeId: 1, title: `压测订单#${roundNo}`, description: '压测自动生成',
      deliveryAddress: '压测收发点', campus: '主校区', rewardAmount: 50, payMethod: 2,
    }, user)).j;
    await api('POST', `/user/order/pay/${sub.data.orderNumber}`, undefined, user);
    const orderId = sub.data.id;

    //同一毫秒级时刻并发抢单
    const t0 = performance.now();
    const results = await Promise.all(skillers.map(async (tk) => {
      const r = await api('POST', '/skiller/orders/grab', { id: orderId }, tk);
      return { code: r.j?.code, msg: r.j?.msg || '', ms: r.ms };
    }));
    const wall = performance.now() - t0;
    const ok = results.filter(r => r.code === 1);
    const latencies = results.map(r => r.ms).sort((a, b) => a - b);
    const pct = (p) => latencies[Math.min(latencies.length - 1, Math.floor(latencies.length * p))];
    if (ok.length !== 1) throw new Error(`第${roundNo}轮成功数=${ok.length}（应为 1）`);
    return {
      round: roundNo, wallMs: Math.round(wall), qps: +(CONCURRENCY / (wall / 1000)).toFixed(1),
      ok: ok.length, avgMs: Math.round(latencies.reduce((s, x) => s + x, 0) / latencies.length),
      p50: Math.round(pct(0.5)), p95: Math.round(pct(0.95)), max: Math.round(latencies[latencies.length - 1]),
      msgTop: [...results.reduce((m, r) => m.set(r.msg, (m.get(r.msg) || 0) + 1), new Map())]
        .sort((a, b) => b[1] - a[1]).slice(0, 3).map(([m, c]) => `${m}×${c}`),
    };
  }

  console.log(`== 预热 ${WARMUP} 轮（不计入统计）==`);
  for (let r = 1; r <= WARMUP; r++) console.log('  warmup', await oneRound(1000 + r));

  console.log(`== 正式压测 ${ROUNDS} 轮 × ${CONCURRENCY} 并发 ==`);
  const rounds = [];
  for (let r = 1; r <= ROUNDS; r++) {
    if (r > 1) {
      //抢单限流窗口为 10 秒/人，轮间隔 10.5s 避免把限流拒绝混入压测指标
      await new Promise(res => setTimeout(res, 10500));
    }
    const row = await oneRound(r);
    rounds.push(row);
    console.log(`  round${r}: QPS=${row.qps} avg=${row.avgMs}ms p95=${row.p95}ms max=${row.max}ms 成功=${row.ok}`);
  }
  const agg = {
    label: LABEL, concurrency: CONCURRENCY, rounds: ROUNDS, at: new Date().toISOString(),
    qps: +(rounds.reduce((s, r) => s + r.qps, 0) / ROUNDS).toFixed(1),
    avgMs: Math.round(rounds.reduce((s, r) => s + r.avgMs, 0) / ROUNDS),
    p95: Math.round(rounds.reduce((s, r) => s + r.p95, 0) / ROUNDS),
    max: Math.max(...rounds.map(r => r.max)),
    correctness: '每轮恰好1人抢单成功',
    detail: rounds,
  };
  const out = join(dirname(fileURLToPath(import.meta.url)), 'loadtest', `${LABEL}.json`);
  mkdirSync(dirname(out), { recursive: true });
  writeFileSync(out, JSON.stringify(agg, null, 2));
  console.log(`\n汇总: QPS=${agg.qps} avg=${agg.avgMs}ms p95=${agg.p95}ms max=${agg.max}ms → ${out}`);
}

main().catch(e => { console.error('压测失败:', e.message); process.exit(1); });
