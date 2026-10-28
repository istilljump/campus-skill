// 三端联调排查：按 web-app/H5 前端实际调用方式逐接口验证
// 检查项：请求参数格式、返回码、返回数据形状（前端依赖的字段）
const BASE = 'http://localhost:8080';
// 运行级独立账号：避免历史运行的钱包/订单状态影响断言
const RUN = Date.now().toString().slice(-7);
const issues = [];
const T = (name, cond, detail) => {
  console.log((cond ? '  ok   ' : '  FAIL ') + name + (cond ? '' : ' :: ' + JSON.stringify(detail).slice(0, 240)));
  if (!cond) issues.push({ name, detail });
};
const api = async (m, u, b, t, h = 'authentication') => {
  const r = await fetch(BASE + u, {
    method: m,
    headers: { 'Content-Type': 'application/json', ...(t ? { [h]: t } : {}) },
    body: b === undefined || b === null ? undefined : JSON.stringify(b),
  });
  let j;
  try { j = await r.json(); } catch { j = { code: -1, raw: (await r.text()).slice(0, 120) }; }
  return j;
};

console.log('== 1. 认证与账号 ==');
const user = await api('POST', '/user/user/login', { code: 'it_u1_' + RUN });
T('用户体验码登录返回 token', user?.code === 1 && typeof user?.data?.token === 'string', user);
const UT = user.data.token;
const sk = await api('POST', '/skiller/auth/login', { code: 'demo2' });
T('技能者登录返回 token', sk?.code === 1 && !!sk?.data?.token, sk);
const SKT = sk.data.token;
const prof = await api('GET', '/user/user/profile', null, UT);
T('profile 含 name/creditScore/publishOrderCount 且不泄露密码', prof?.code === 1 &&
  ['name', 'creditScore', 'publishOrderCount'].every(k => k in prof.data) && prof.data.password === undefined, prof);
console.log('   profile.username =', JSON.stringify(prof.data.username));

console.log('== 2. 类目/地址/大厅/市场 ==');
const cats = await api('GET', '/user/skillCategory/list', null, UT);
T('类目列表为数组且含 id/name/feeRate', cats?.code === 1 && Array.isArray(cats.data) && cats.data[0]?.feeRate !== undefined, cats);
const addr = await api('GET', '/user/addressBook/list', null, UT);
T('地址列表为数组', addr?.code === 1 && Array.isArray(addr.data), addr);
const hall = await api('GET', '/user/hall/list?page=1&pageSize=12', null, UT);
T('大厅 PageResult 形状 records/total', hall?.code === 1 && Array.isArray(hall?.data?.records) && 'total' in hall.data, hall);
const hallEmptyKw = await api('GET', '/user/hall/list?page=1&pageSize=12&keyword=&typeId=', null, UT);
T('大厅空参数 keyword=&typeId= 不报错', hallEmptyKw?.code === 1, hallEmptyKw);
const mkt = await api('GET', '/user/market?page=1&pageSize=12&keyword=&categoryId=&sort=default', null, UT);
T('市场空参数 keyword=&categoryId= 不报错且形状正确', mkt?.code === 1 && Array.isArray(mkt?.data?.records), mkt);
const mktSorted = await api('GET', '/user/market?page=1&pageSize=12&sort=price', null, UT);
T('市场 sort=price 生效', mktSorted?.code === 1, mktSorted);

console.log('== 3. 发单→支付→详情→追加→返修→验收→评价 ==');
const u2 = await api('POST', '/user/user/login', { code: 'it_u2_' + RUN });
const UT2 = u2.data.token;
const sub = await api('POST', '/user/order/submit', {
  title: '联调测试单', description: '集成验证', deliveryAddress: '线上交付',
  rewardAmount: 20, payMethod: 2, typeId: 1, expectedTime: '2026-10-15 14:00:00',
}, UT2);
T('发单返回 orderNumber', sub?.code === 1 && !!sub?.data?.orderNumber, sub);
const onum = sub.data.orderNumber;
const pay1 = await api('POST', `/user/order/pay/${onum}`, null, UT2);
console.log('   首次支付(新用户0余额):', pay1?.code === 1 ? '成功' : pay1?.msg);
const rech = await api('POST', '/user/wallet/recharge', { amount: 50 }, UT2);
T('充值50', rech?.code === 1, rech);
const pay2 = await api('POST', `/user/order/pay/${onum}`, null, UT2);
T('充值后支付成功', pay2?.code === 1, pay2);
const pg = await api('GET', '/user/order/page?page=1&pageSize=5&status=2', null, UT2);
const oid = (pg.data.records || [])[0]?.id;
T('订单分页 status 过滤可用', !!oid, pg);
const det = await api('GET', `/user/order/detail/${oid}`, null, UT2);
T('订单详情含 number/rewardAmount/platformFee/orderTime/status', det?.code === 1 &&
  ['number', 'rewardAmount', 'platformFee', 'orderTime', 'status'].every(k => k in det.data), det?.data && Object.keys(det.data));
const boost = await api('PUT', '/user/order/boost', { id: oid, amount: 5 }, UT2);
T('追加悬赏', boost?.code === 1, boost);
const rv0 = await api('GET', `/user/review/order/${oid}`, null, UT2);
T('未评价订单 review 查询可判空', rv0?.code === 1, rv0);

console.log('== 4. 技能者链路 ==');
const skHall = await api('GET', '/skiller/orders/hall?page=1&pageSize=20', null, SKT);
T('技能者大厅（新单在hall中或空）', skHall?.code === 1 && Array.isArray(skHall.data.records), skHall);
const grab = await api('POST', '/skiller/orders/grab', { id: oid }, SKT);
T('抢单成功', grab?.code === 1, grab);
const dupGrab = await api('POST', '/skiller/orders/grab', { id: oid }, SKT);
T('重复抢单被拒', dupGrab?.code === 0, dupGrab);
const pickup = await api('PUT', `/skiller/orders/pickup/${oid}`, null, SKT);
T('确认开始服务', pickup?.code === 1, pickup);
const deliver = await api('PUT', `/skiller/orders/deliver/${oid}`, { deliverableUrl: 'https://example.com/d.zip', deliverableNote: '联调交付' }, SKT);
T('交付成果', deliver?.code === 1, deliver);
const rework = await api('PUT', `/user/order/rework/${oid}`, { reason: '联调返修' }, UT2);
T('用户申请返修', rework?.code === 1, rework);
const respond = await api('POST', `/skiller/orders/rework/${oid}`, null, SKT);
T('技能者响应返修', respond?.code === 1, respond);
const deliver2 = await api('PUT', `/skiller/orders/deliver/${oid}`, { deliverableUrl: 'https://example.com/d2.zip' }, SKT);
T('二次交付', deliver2?.code === 1, deliver2);
const accept = await api('PUT', `/user/order/accept/${oid}`, null, UT2);
T('用户验收完成', accept?.code === 1, accept);
const review = await api('POST', '/user/review/submit', { orderId: oid, score: 5, content: '很好', tags: '', isAnonymous: 0 }, UT2);
T('提交评价', review?.code === 1, review);
const rv1 = await api('GET', `/user/review/order/${oid}`, null, UT2);
T('已评价查询返回 id', rv1?.code === 1 && !!rv1?.data?.id, rv1);
const skw = await api('GET', '/skiller/wallet/balance', null, SKT);
T('技能者钱包含 balance/totalIncome', skw?.code === 1 && 'balance' in skw.data && 'totalIncome' in skw.data, skw);
const skTrend = await api('GET', '/skiller/info/trend?days=7', null, SKT);
T('技能者7日趋势含 date/amount', skTrend?.code === 1 && (skTrend.data[0] ? ('date' in skTrend.data[0] && 'amount' in skTrend.data[0]) : true), skTrend);
const skCenter = await api('GET', '/skiller/info/center', null, SKT);
T('技能者中心字段完整', skCenter?.code === 1 &&
  ['completedOrders', 'creditScore', 'dailyOrderLimit', 'todayOrderCount'].every(k => k in skCenter.data), skCenter?.data && Object.keys(skCenter.data));

console.log('== 5. 预约链路 ==');
// 取「当前技能者自己上架的服务」，避免依赖市场排序（排序会随演示数据变化）
const skSvcs = await api('GET', '/skiller/service/list', null, SKT);
const svc = (skSvcs.data || []).find(s => s.status === 1) || (skSvcs.data || [])[0];
const bk = await api('POST', '/user/booking', { serviceItemId: svc.id, expectTime: '2026-10-20 14:00', remark: '联调预约' }, UT);
T('创建预约', bk?.code === 1, bk);
const skb = await api('GET', '/skiller/booking/page?page=1&pageSize=20&status=0', null, SKT);
// 按本次联调备注定位自己刚建的预约单，避免误操作到演示数据
const target = (skb.data.records || []).find(b => b.remark === '联调预约');
T('技能者可见待确认预约（含 userName/skillerName）', !!target && !!target.userName && !!target.skillerName, skb);
const cf = await api('POST', `/skiller/booking/${target.id}/confirm`, null, SKT);
T('技能者确认预约', cf?.code === 1, cf);
const bpay = await api('POST', `/user/booking/${target.id}/pay`, null, UT);
T('预约下单返回 orderNumber', bpay?.code === 1 && !!bpay?.data?.orderNumber, bpay);
const bpayOrder = await api('POST', `/user/order/pay/${bpay.data.orderNumber}`, null, UT);
T('预约订单支付', bpayOrder?.code === 1 || (bpayOrder?.msg || '').includes('余额'), bpayOrder);

console.log('== 6. 消息/钱包/地址 ==');
const unread = await api('GET', '/user/message/unread', null, UT);
T('用户未读数', unread?.code === 1 && typeof unread.data === 'number', unread);
const msgs = await api('GET', '/user/message/list', null, UT);
T('消息列表字段完整', msgs?.code === 1 && (msgs.data[0] ? ['title', 'isRead', 'createTime'].every(k => k in msgs.data[0]) : true), msgs);
const skMsg = await api('GET', '/skiller/message/list', null, SKT);
T('技能者消息列表', skMsg?.code === 1, skMsg);
const txs = await api('GET', '/user/wallet/transactions?page=1&pageSize=20', null, UT);
T('钱包流水字段', txs?.code === 1 && (txs.data.records[0] ? 'type' in txs.data.records[0] : true), txs);
const addNew = await api('POST', '/user/addressBook', { consignee: '联调', phone: '13800000000', campus: '主校区', building: 'T1', room: '101', sex: '1', isDefault: 0 }, UT);
T('新增地址', addNew?.code === 1, addNew);
const addrList = await api('GET', '/user/addressBook/list', null, UT);
const newAddr = (addrList.data || []).find(a => a.consignee === '联调');
T('新地址出现在列表', !!newAddr, addrList);
const del = await api('DELETE', '/user/addressBook?id=' + newAddr.id, null, UT);
T('删除地址', del?.code === 1, del);

console.log('== 7. 技能者工作台数据源 ==');
const skSvc = await api('GET', '/skiller/service/list', null, SKT);
T('服务列表（数组或PageResult）', skSvc?.code === 1, skSvc);
const skPf = await api('GET', '/skiller/portfolio', null, SKT);
T('作品列表', skPf?.code === 1, skPf);
const skWd = await api('GET', '/skiller/wallet/withdraw/page?page=1&pageSize=10', null, SKT);
T('提现记录分页', skWd?.code === 1 && Array.isArray(skWd?.data?.records), skWd);
const skTx = await api('GET', '/skiller/wallet/transactions?page=1&pageSize=10', null, SKT);
T('技能者流水', skTx?.code === 1, skTx);

console.log('\n== 联调排查完成：' + issues.length + ' 个疑似问题 ==');
issues.forEach((it, i) => console.log((i + 1) + '. ' + it.name + ' :: ' + JSON.stringify(it.detail).slice(0, 200)));
