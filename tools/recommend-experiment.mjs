// CampusSkill 推荐效果对比实验（v8 论文素材）
//
// 实验设计（模拟偏好驱动用户）：
//  1. 为每个实验用户注入一段"兴趣历史"：连续浏览某类目（C）下的服务 → 激活推荐引擎的类目画像；
//  2. 曝光阶段：每个用户在两种来源下各获得 K 次曝光——
//     - 市场列表（对照组，src=1）：取默认销量排序的靠前服务（与用户兴趣无关）；
//     - 猜你喜欢（实验组，src=2）：取推荐接口返回的靠前服务（应偏向用户兴趣类目 C）；
//  3. 转化规则：用户只对类目 C 的服务产生"预约"（模拟真实兴趣决策）；
//  4. 统计两组的预约转化率（转化数/曝光数）与曝光命中类目 C 的比例（@hitRate）。
//
// 若推荐引擎有效，实验组的命中类目比例与转化率应显著高于对照组。
// 用法：node tools/recommend-experiment.mjs --users 10 --expose 6
import { writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const BASE = process.env.BASE_URL || 'http://localhost:8080';
const args = process.argv.slice(2);
const arg = (k, d) => { const i = args.indexOf('--' + k); return i >= 0 ? Number(args[i + 1]) : d; };
const USERS = arg('users', 10);
const EXPOSE = arg('expose', 6);   // 每组曝光次数

async function api(method, url, body, token, headerName = 'authentication') {
  const res = await fetch(BASE + url, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { [headerName]: token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  return res.json().catch(() => ({}));
}

async function main() {
  const tag = 'exp' + Date.now().toString(36).slice(-5);
  console.log(`== 推荐效果实验：${USERS} 用户 × 每组 ${EXPOSE} 次曝光 ==`);

  let recViews = 0, recHits = 0, recConv = 0;
  let mktViews = 0, mktHits = 0, mktConv = 0;

  for (let u = 0; u < USERS; u++) {
    const token = (await api('POST', '/user/user/login', { code: `${tag}_${u}` })).data.token;
    //随机给该用户指定一个兴趣类目
    const market = (await api('GET', '/user/market?page=1&pageSize=50', undefined, token)).data?.records || [];
    if (market.length < 5) throw new Error('市场上架服务不足，先导入演示数据');
    const categories = [...new Set(market.map(s => s.categoryId))];
    const interest = categories[u % categories.length];

    //阶段1：注入兴趣历史（浏览 3 个该类目服务，来源=4 其他）
    const interestItems = market.filter(s => s.categoryId === interest).slice(0, 3);
    for (const it of interestItems) {
      await api('GET', `/user/market/${it.id}?src=4`, undefined, token);
    }

    //阶段2a：实验组——猜你喜欢曝光
    const rec = (await api('GET', '/user/market/recommend?limit=10', undefined, token)).data || [];
    for (const it of rec.slice(0, EXPOSE)) {
      recViews++;
      if (it.categoryId === interest) recHits++;
      await api('GET', `/user/market/${it.id}?src=2`, undefined, token);
      if (it.categoryId === interest) {
        //兴趣命中 → 预约（转化）；预约行为继承来源 src=2
        const bk = await api('POST', '/user/booking', {
          serviceItemId: it.id, expectTime: '2026-11-05 14:00', remark: '推荐效果实验', src: 2
        }, token);
        if (bk?.code === 1) recConv++;
      }
    }

    //阶段2b：对照组——市场默认排序曝光（取未浏览过的销量靠前服务）
    const seen = new Set([...rec.map(r => r.id), ...interestItems.map(i => i.id)]);
    const mkt = market.filter(s => !seen.has(s.id));
    for (const it of mkt.slice(0, EXPOSE)) {
      mktViews++;
      if (it.categoryId === interest) mktHits++;
      await api('GET', `/user/market/${it.id}?src=1`, undefined, token);
      if (it.categoryId === interest) {
        const bk = await api('POST', '/user/booking', {
          serviceItemId: it.id, expectTime: '2026-11-05 14:00', remark: '推荐效果实验', src: 1
        }, token);
        if (bk?.code === 1) mktConv++;
      }
    }
  }

  const pct = (a, b) => b === 0 ? '-' : (100 * a / b).toFixed(1) + '%';
  const result = {
    at: new Date().toISOString(), users: USERS, exposePerGroup: EXPOSE,
    recommend: { views: recViews, interestHits: recHits, hitRate: pct(recHits, recViews), conversions: recConv, ctr: pct(recConv, recViews) },
    market: { views: mktViews, interestHits: mktHits, hitRate: pct(mktHits, mktViews), conversions: mktConv, ctr: pct(mktConv, mktViews) },
  };
  console.log('\n===== 实验结果 =====');
  console.log(`猜你喜欢: 曝光 ${recViews} · 兴趣类目命中 ${recHits}（${result.recommend.hitRate}）· 预约转化 ${recConv}（${result.recommend.ctr}）`);
  console.log(`市场列表: 曝光 ${mktViews} · 兴趣类目命中 ${mktHits}（${result.market.hitRate}）· 预约转化 ${mktConv}（${result.market.ctr}）`);
  const out = join(dirname(fileURLToPath(import.meta.url)), 'loadtest', 'recommend-experiment.json');
  mkdirSync(dirname(out), { recursive: true });
  writeFileSync(out, JSON.stringify(result, null, 2));
  console.log(`\n结果已写入 ${out}（管理端 /admin/statistics/recommendEffect 可查看平台侧统计）`);
}

main().catch(e => { console.error('实验失败:', e.message); process.exit(1); });
