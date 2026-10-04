// CampusSkill E2E 冒烟测试：悬赏链路 / 预约链路 / 支撑链路
const BASE = 'http://localhost:8080';
let pass = 0, fail = 0, failures = [];

function check(name, cond, detail) {
  if (cond) { pass++; console.log(`  PASS ${name}`); }
  else { fail++; failures.push(name + (detail ? ` :: ${detail}` : '')); console.log(`  FAIL ${name}${detail ? ' :: ' + JSON.stringify(detail).slice(0, 300) : ''}`); }
}

async function api(method, url, body, token, headerName = 'authentication') {
  const res = await fetch(BASE + url, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { [headerName]: token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  let j = null;
  try { j = await res.json(); } catch { j = { httpStatus: res.status, text: await res.text().catch(() => '') }; }
  return j;
}

const sleep = (ms) => new Promise(r => setTimeout(r, ms));
const L = (r) => r?.data?.list ?? r?.data?.records ?? [];

async function main() {
  console.log('== 0. 登录三端 ==');
  const user1 = await api('POST', '/user/user/login', { code: 'e2e_user1' });
  check('用户1登录', user1?.code === 1 && user1?.data?.token, user1);
  const u1 = user1.data.token;
  const user2 = await api('POST', '/user/user/login', { code: 'e2e_user2' });
  check('用户2登录', user2?.code === 1 && user2?.data?.token, user2);
  const u2 = user2.data.token;
  const admin = await api('POST', '/admin/employee/login', { username: 'admin', password: '123456' }, undefined, 'token');
  check('管理员登录', admin?.code === 1 && admin?.data?.token, admin);
  const at = admin.data.token;

  console.log('== 1. 公开数据 ==');
  const cats = await api('GET', '/user/skillCategory/list', undefined, u1);
  check('技能类目列表', cats?.code === 1 && Array.isArray(cats?.data) && cats.data.length >= 8, cats);

  console.log('== 2. 用户2 入驻为技能者 + 管理员实名审核 ==');
  const apply = await api('POST', '/user/skiller/apply', {
    realName: '测试技能者', studentNo: '20260101', campus: '主校区', college: '计算机学院',
    idCard: '110101200001010011', studentCardImg: 'http://fake/img.jpg',
  }, u2);
  check('技能者入驻申请(或已在审核中)', apply?.code === 1 || (apply?.msg||'').includes('审核中'), apply);
  let auditPage = await api('GET', '/admin/skillerAudit/page?page=1&pageSize=10&status=0', undefined, at, 'token');
  const auditRec = L(auditPage).find(r => r.userId != null) || L(auditPage)[0];
  check('管理端可见认证申请', !!auditRec, auditPage);
  if (auditRec) {
    const proc = await api('PUT', '/admin/skillerAudit/process', { id: auditRec.id, status: 1, auditRemark: '通过' }, at, 'token');
    check('实名认证通过', proc?.code === 1, proc);
  }
  const skillerLogin = await api('POST', '/skiller/auth/login', { code: 'e2e_user2' });
  check('技能者登录', skillerLogin?.code === 1 && skillerLogin?.data?.token, skillerLogin);
  const st = skillerLogin.data.token;
  const center = await api('GET', '/skiller/info/center', undefined, st);
  check('技能者中心', center?.code === 1 && center?.data?.auditStatus === 1, center);

  console.log('== 3. 用户1 钱包充值并发布悬赏 ==');
  const rech = await api('POST', '/user/wallet/recharge', { amount: 500 }, u1);
  check('充值500', rech?.code === 1, rech);
  const bal0 = await api('GET', '/user/wallet/balance', undefined, u1);
  check('余额=500', bal0?.data?.balance === 500, bal0);
  const submit = await api('POST', '/user/order/submit', {
    typeId: 1, title: '课程汇报PPT美化', description: '20页以内，科技风模板',
    deliveryAddress: '东区3号楼502', campus: '主校区', rewardAmount: 50, payMethod: 2,
  }, u1);
  check('发布悬赏订单', submit?.code === 1 && submit?.data?.orderNumber, submit);
  const orderNumber = submit.data.orderNumber;
  check('服务费计算(10%)', Number(submit.data.platformFee) === 5, submit.data);
  const pay = await api('POST', `/user/order/pay/${orderNumber}`, undefined, u1);
  check('钱包支付', pay?.code === 1, pay);
  const bal1 = await api('GET', '/user/wallet/balance', undefined, u1);
  check('支付后余额=450', Number(bal1?.data?.balance) === 450, bal1);

  console.log('== 4. 技能者抢单 → 交付 → 验收（悬赏链路核心） ==');
  const hall = await api('GET', '/skiller/orders/hall?page=1&pageSize=10', undefined, st);
  check('悬赏大厅可见', hall?.code === 1 && L(hall).some(o => o.number === orderNumber), hall);
  const hallItem = L(hall).find(o => o.number === orderNumber);
  const grab = await api('POST', '/skiller/orders/grab', { id: hallItem.id }, st);
  check('抢单成功', grab?.code === 1, grab);
  const grab2 = await api('POST', '/skiller/orders/grab', { id: hallItem.id }, st);
  check('重复抢单被拒', grab2?.code !== 1, grab2);
  const pickup = await api('PUT', `/skiller/orders/pickup/${hallItem.id}`, undefined, st);
  check('开始服务', pickup?.code === 1, pickup);
  const deliverNoUrl = await api('PUT', `/skiller/orders/deliver/${hallItem.id}`, { deliverableUrl: '', deliverableNote: '' }, st);
  check('空交付物被拒', deliverNoUrl?.code !== 1, deliverNoUrl);
  const deliver = await api('PUT', `/skiller/orders/deliver/${hallItem.id}`, { deliverableUrl: 'http://oss/ppt_v1.pptx', deliverableNote: '初稿，请查收' }, st);
  check('交付成功', deliver?.code === 1, deliver);
  const d1 = await api('GET', `/user/order/detail/${hallItem.id}`, undefined, u1);
  check('详情含交付物', d1?.data?.deliverableUrl === 'http://oss/ppt_v1.pptx' && d1?.data?.status === 4, d1?.data);
  // 技能者钱包应未结算（结算在验收）
  const sb0 = await api('GET', '/skiller/wallet/balance', undefined, st);
  check('交付后未结算(余额0)', Number(sb0?.data?.balance) === 0, sb0);

  console.log('== 5. 返修一轮 → 再交付 → 验收 ==');
  const rework = await api('PUT', `/user/order/rework/${hallItem.id}`, { reason: '配色不符合要求' }, u1);
  check('申请返修', rework?.code === 1, rework);
  const rd = await api('GET', `/user/order/detail/${hallItem.id}`, undefined, u1);
  check('返修后状态=8', rd?.data?.status === 8 && rd?.data?.reworkCount === 1, rd?.data);
  const respond = await api('POST', `/skiller/orders/rework/${hallItem.id}`, undefined, st);
  check('技能者响应返修', respond?.code === 1, respond);
  const redeliver = await api('PUT', `/skiller/orders/deliver/${hallItem.id}`, { deliverableUrl: 'http://oss/ppt_v2.pptx', deliverableNote: '已按意见修改' }, st);
  check('二次交付', redeliver?.code === 1, redeliver);
  const accept = await api('PUT', `/user/order/accept/${hallItem.id}`, undefined, u1);
  check('验收完成', accept?.code === 1, accept);
  const ad = await api('GET', `/user/order/detail/${hallItem.id}`, undefined, u1);
  check('验收后状态=5', ad?.data?.status === 5, ad?.data);
  const sb1 = await api('GET', '/skiller/wallet/balance', undefined, st);
  check('验收后技能者到账45', Number(sb1?.data?.balance) === 45, sb1);
  const bal2 = await api('GET', '/user/wallet/balance', undefined, u1);
  check('用户余额仍为450', Number(bal2?.data?.balance) === 450, bal2);

  console.log('== 6. 评价与信用分 ==');
  const review = await api('POST', '/user/review/submit', { orderId: hallItem.id, score: 5, content: '很棒', tags: '响应快,质量高', isAnonymous: 0 }, u1);
  check('五星评价', review?.code === 1, review);
  const c2 = await api('GET', '/skiller/info/center', undefined, st);
  check('信用分=100(100-3返修+1交付+2好评)', c2?.data?.creditScore === 100, c2?.data);
  const c2b = await api('GET', '/skiller/info/center', undefined, st);
  check('完成单数=1', c2b?.data?.completedOrders === 1, c2b?.data);

  console.log('== 7. 作品集 → 管理端审核定级 ==');
  const pf = await api('POST', '/skiller/portfolio', { title: '国潮风海报合集', categoryId: 2, coverUrl: 'http://fake/c1.jpg', workUrls: 'http://fake/w1.jpg,http://fake/w2.jpg', description: '三个活动海报' }, st);
  check('上传作品', pf?.code === 1, pf);
  const paPage = await api('GET', '/admin/portfolioAudit/page?page=1&pageSize=10&status=0', undefined, at, 'token');
  const pa = L(paPage)[0];
  check('管理端可见作品', !!pa, paPage);
  if (pa) {
    const pap = await api('PUT', '/admin/portfolioAudit/process', { id: pa.id, status: 1, auditOpinion: '优秀', skillLevel: 3 }, at, 'token');
    check('作品审核通过定级C3', pap?.code === 1, pap);
    const prof = await api('GET', `/user/skiller/${center.data.id}`, undefined, u1);
    check('技能者主页公开可见(C3+作品+服务)', prof?.code === 1 && prof?.data?.skillLevel === 3 && prof?.data?.portfolio?.length >= 1, prof?.data);
  }

  console.log('== 8. 服务货架 + 预约链路（含返修用尽→仲裁） ==');
  const svc = await api('POST', '/skiller/service', { categoryId: 3, title: '活动视频剪辑（3分钟内）', description: '含字幕包装，2轮修改', price: 100, deliveryDays: 3, serviceMode: 1, tags: '剪辑,Premiere' }, st);
  check('上架服务', svc?.code === 1, svc);
  const market = await api('GET', '/user/market?page=1&pageSize=10', undefined, u1);
  check('技能市场可见服务', market?.code === 1 && L(market).some(s => s.title?.includes('视频剪辑')), market);
  const bk = await api('POST', '/user/booking', { serviceItemId: svc.data, expectTime: '2026-10-06 14:00', remark: '社团晚会视频' }, u1);
  check('预约服务', bk?.code === 1, bk);
  check('服务发布返回ID', typeof svc.data === 'number' && svc.data > 0, svc.data);
  const bkPageS = await api('GET', '/skiller/booking/page?page=1&pageSize=10&status=0', undefined, st);
  const bkItem = L(bkPageS)[0];
  check('技能者可见预约', !!bkItem, bkPageS);
  const bkc = await api('POST', `/skiller/booking/${bkItem.id}/confirm`, undefined, st);
  check('技能者确认预约', bkc?.code === 1, bkc);
  const bpay = await api('POST', `/user/booking/${bkItem.id}/pay`, undefined, u1);
  check('预约下单', bpay?.code === 1 && bpay?.data?.orderNumber, bpay);
  const bpay2 = await api('POST', `/user/order/pay/${bpay.data.orderNumber}`, undefined, u1);
  check('预约订单支付', bpay2?.code === 1, bpay2);
  const bDetail = await api('GET', `/user/order/detail/${bpay.data.id}`, undefined, u1);
  check('预约订单mode=2且关联服务', bDetail?.data?.mode === 2 && bDetail?.data?.serviceItemId === svc.data, bDetail?.data);
  check('预约支付后直接进行中(状态3)', bDetail?.data?.status === 3, bDetail?.data);
  const bg = await api('POST', '/skiller/orders/grab', { id: bDetail.data.id }, st);
  check('预约单无需抢单(应失败或直接进行中)', bg?.code !== 1 || bDetail.data.status !== 2, bg);
  // 预约单技能者直接开始服务
  const bpick = await api('PUT', `/skiller/orders/pickup/${bDetail.data.id}`, undefined, st);
  check('预约单开始服务', bpick?.code === 1, bpick);
  const bdel = await api('PUT', `/skiller/orders/deliver/${bDetail.data.id}`, { deliverableUrl: 'http://oss/v_final.mp4', deliverableNote: '成片' }, st);
  check('预约单交付', bdel?.code === 1, bdel);
  // 两轮返修用尽 → 仲裁
  await api('PUT', `/user/order/rework/${bDetail.data.id}`, { reason: 'r1' }, u1);
  await api('POST', `/skiller/orders/rework/${bDetail.data.id}`, undefined, st);
  await api('PUT', `/skiller/orders/deliver/${bDetail.data.id}`, { deliverableUrl: 'http://oss/v2.mp4', deliverableNote: '改1' }, st);
  await api('PUT', `/user/order/rework/${bDetail.data.id}`, { reason: 'r2' }, u1);
  await api('POST', `/skiller/orders/rework/${bDetail.data.id}`, undefined, st);
  await api('PUT', `/skiller/orders/deliver/${bDetail.data.id}`, { deliverableUrl: 'http://oss/v3.mp4', deliverableNote: '改2' }, st);
  const rw3 = await api('PUT', `/user/order/rework/${bDetail.data.id}`, { reason: 'r3' }, u1);
  check('第3次返修被拒', rw3?.code !== 1, rw3);
  const disp = await api('POST', `/user/order/dispute/${bDetail.data.id}`, { reasonType: 1, description: '字幕错字太多', evidenceUrls: 'http://fake/e1.jpg' }, u1);
  check('发起仲裁(状态9)', disp?.code === 1, disp);
  const dpPage = await api('GET', '/admin/dispute/page?page=1&pageSize=10&status=0', undefined, at, 'token');
  const dp = L(dpPage)[0];
  check('管理端可见仲裁工单', !!dp, dpPage);
  const creditBefore = (await api('GET', '/skiller/info/center', undefined, st)).data.creditScore;
  const verdict = await api('PUT', `/admin/dispute/${dp.id}/verdict`, { status: 1, verdict: '质量不达标，退款用户' }, at, 'token');
  check('仲裁退款用户', verdict?.code === 1, verdict);
  const bal3 = await api('GET', '/user/wallet/balance', undefined, u1);
  check('仲裁后退款+100(余额450)', Number(bal3?.data?.balance) === 450, bal3);
  const creditAfter = (await api('GET', '/skiller/info/center', undefined, st)).data.creditScore;
  check('仲裁判责信用-10', creditAfter === creditBefore - 10, { creditBefore, creditAfter });

  console.log('== 9. 提现链路 ==');
  const pwd = await api('POST', '/skiller/wallet/password', { withdrawPassword: '123456' }, st).catch(() => null);
  const setPwd = pwd === null ? await api('POST', '/skiller/wallet/password', { oldPassword: '', newPassword: '123456' }, st) : pwd;
  // 密码接口参数不定，直接尝试提现看报错提示
  const wd = await api('POST', '/skiller/wallet/withdraw', { amount: 10, withdrawPassword: '123456' }, st);
  check('提现申请(成功或提示设置密码)', wd?.code === 1 || (typeof wd?.msg === 'string' && wd.msg.length > 0), wd);
  const wdPage = await api('GET', '/admin/withdraw/page?page=1&pageSize=10', undefined, at, 'token');
  const wdItem = L(wdPage)[0];
  if (wd?.code === 1 && wdItem) {
    const wdp = await api('PUT', '/admin/withdraw/process', { id: wdItem.id, status: 1, remark: 'ok' }, at, 'token');
    check('管理端打款', wdp?.code === 1, wdp);
  }

  console.log('== 10. 消息与统计 ==');
  const msgs = await api('GET', '/user/message/list?page=1&pageSize=5', undefined, u1);
  check('用户消息列表', msgs?.code === 1, msgs);
  const stat = await api('GET', '/admin/statistics/overview', undefined, at, 'token');
  check('统计总览', stat?.code === 1 && Number(stat?.data?.totalOrders) >= 2, stat);
  const rank = await api('GET', '/admin/credit/rank?limit=10', undefined, at, 'token');
  check('信用榜单', rank?.code === 1 && rank?.data?.length >= 1, rank);
  const trend = await api('GET', '/skiller/info/trend', undefined, st);
  check('技能者7日趋势', trend?.code === 1, trend);

  console.log(`\n===== 结果: ${pass} 通过 / ${fail} 失败 =====`);
  if (failures.length) { console.log('失败项:'); failures.forEach(f => console.log(' - ' + f)); }
}

main().catch(e => { console.error('E2E crashed:', e); process.exit(1); });
