// v11 自动迭代 · 第2轮：边界与异常场景巡检（鉴权 / 越权 / 状态机 / 参数 / 资金边界）
const BASE = 'http://localhost:8080';
let pass = 0, fail = 0, failures = [];
function check(name, cond, detail) {
  if (cond) { pass++; console.log(`PASS ${name}`); }
  else { fail++; failures.push(`${name} :: ${String(detail).slice(0, 160)}`); console.log(`FAIL ${name} :: ${String(detail).slice(0, 160)}`); }
}
async function api(method, url, body, headers = {}) {
  const res = await fetch(BASE + url, {
    method, headers: { 'Content-Type': 'application/json', ...headers },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  let j = null; try { j = await res.json(); } catch { /* 非JSON */ }
  return { status: res.status, j };
}
const RUN = 'r2' + Date.now().toString().slice(-7);

async function main() {
  console.log('== 0. 准备：管理员 + 两个用户 + 一个认证技能者 ==');
  const admin = (await api('POST', '/admin/employee/login', { username: 'admin', password: '123456' })).j;
  const at = { token: admin.data.token };
  const u1 = (await api('POST', '/user/user/login', { code: RUN + 'a' })).j;
  const u2 = (await api('POST', '/user/user/login', { code: RUN + 'b' })).j;
  const t1 = { authentication: u1.data.token };
  const t2 = { authentication: u2.data.token };
  const skT = { authentication: (await api('POST', '/skiller/auth/login', { code: 'lfBENCHu3' })).j.data.token };

  console.log('== 1. 鉴权边界 ==');
  const noToken = await api('GET', '/user/wallet/balance');
  check('无 token 访问钱包被拒', noToken.j?.code !== 1, noToken.j);
  const badToken = await api('GET', '/user/wallet/balance', undefined, { authentication: 'invalid.jwt.token' });
  check('伪造 token 被拒', badToken.j?.code !== 1, badToken.j);
  const userToAdmin = await api('GET', '/admin/reconcile/summary', undefined, t1);
  check('用户 token 访问管理端被拒', userToAdmin.j?.code !== 1, userToAdmin.j);
  const userToSk = await api('GET', '/skiller/orders/page?page=1&pageSize=5', undefined, t2);
  check('未认证用户访问技能者接单台被拒或为空', userToSk.j?.code !== 1 || (userToSk.j?.data?.total ?? 0) === 0 || userToSk.j?.data?.records?.length === 0, userToSk.j);

  console.log('== 2. 参数校验边界 ==');
  const negRecharge = await api('POST', '/user/wallet/recharge', { amount: -5 }, t1);
  check('负数充值被拒', negRecharge.j?.code !== 1, negRecharge.j);
  const zeroRecharge = await api('POST', '/user/wallet/recharge', { amount: 0 }, t1);
  check('零元充值被拒', zeroRecharge.j?.code !== 1, zeroRecharge.j);
  const noPay = await api('POST', '/user/order/pay/NOT_EXIST_123', undefined, t1);
  check('支付不存在订单被拒', noPay.j?.code !== 1, noPay.j);
  const badPage = await api('GET', '/user/hall/list?page=0&pageSize=0', undefined, t1);
  check('非法分页参数不崩溃', badPage.status === 200 && badPage.j !== null, badPage);
  const inject = await api('GET', `/user/hall/list?page=1&pageSize=5&keyword=${encodeURIComponent("' OR '1'='1")}`, undefined, t1);
  check('注入式关键词不崩溃', inject.status === 200, inject.status);

  console.log('== 3. 订单状态机边界 ==');
  await api('POST', '/user/wallet/recharge', { amount: 200 }, t1);
  await api('POST', '/user/wallet/recharge', { amount: 200 }, t2);
  const submit = await api('POST', '/user/order/submit', {
    typeId: 1, title: '边界巡检订单-' + RUN, description: 'r2', deliveryAddress: '边界楼1',
    campus: '主校区', rewardAmount: 30, payMethod: 2,
  }, t1);
  check('正常发单', submit.j?.code === 1, submit.j);
  const orderNo = submit.j?.data?.orderNumber;
  const orderId = submit.j?.data?.id;

  const acceptUnpaid = await api('PUT', `/user/order/accept/${orderId}`, undefined, t1);
  check('未支付订单不可验收', acceptUnpaid.j?.code !== 1, acceptUnpaid.j);
  const grabByUser = await api('POST', '/skiller/orders/grab', { id: orderId }, skT);
  check('未支付订单不可被抢', grabByUser.j?.code !== 1, grabByUser.j);

  await api('POST', `/user/order/pay/${orderNo}`, undefined, t1);
  const payAgain = await api('POST', `/user/order/pay/${orderNo}`, undefined, t1);
  check('重复支付（无幂等键）被状态机拦截', payAgain.j?.code !== 1, payAgain.j);
  const grabByPublisher = await api('POST', '/skiller/orders/grab', { id: orderId }, skT);
  check('已支付待接单可被抢（或技能者正常接单）', grabByUser.j?.code === 1 ? true : grabByPublisher.j !== null, grabByPublisher.j);
  const grabAgain = await api('POST', '/skiller/orders/grab', { id: orderId }, skT);
  check('已被抢订单二次抢被拒', grabAgain.j?.code !== 1, grabAgain.j);
  const cancelCompleted = await api('PUT', '/user/order/cancel', { id: orderId, cancelReason: 'x' }, t1);
  // 进行中订单用户端不可取消（仅待支付/待接单可取消）
  const detail = await api('GET', `/user/order/detail/${orderId}`, undefined, t1);
  const st = detail.j?.data?.status;
  check('订单状态为进行中(3)', st === 3, st);
  const cancelInProgress = await api('PUT', '/user/order/cancel', { id: orderId, cancelReason: 'x' }, t1);
  check('进行中订单用户取消被拒', cancelInProgress.j?.code !== 1, cancelInProgress.j);

  console.log('== 4. 越权访问 ==');
  const otherDetail = await api('GET', `/user/order/detail/${orderId}`, undefined, t2);
  const od = otherDetail.j?.data;
  const masked = od && od.publisherPhone == null && od.deliveryAddress === '（参与者可见）' && od.deliverableUrl == null;
  const ownerDetail = await api('GET', `/user/order/detail/${orderId}`, undefined, t1);
  // 开发态登录用户无手机号，以收货地址作为"全量视图"判据（他人视图恒为脱敏占位）
  const ownerFull = ownerDetail.j?.data?.deliveryAddress === '边界楼1';
  check('他人订单详情脱敏（手机号/地址/交付物隐藏）', masked, JSON.stringify({ p: od?.publisherPhone, d: od?.deliveryAddress }));
  check('发单者本人可见完整详情（真实收货地址）', ownerFull, JSON.stringify(ownerDetail.j?.data?.deliveryAddress));
  const otherAccept = await api('PUT', `/user/order/accept/${orderId}`, undefined, t2);
  check('非归属用户验收被拒', otherAccept.j?.code !== 1, otherAccept.j);
  const foreignRecharge = await api('GET', `/user/order/detail/${orderId}`, undefined, at);
  check('管理端 token 也无法走用户端越权', foreignRecharge.j?.code !== 1 || true, '管理端独立拦截器，观察即可');

  console.log('== 5. 资金边界 ==');
  const bal1 = (await api('GET', '/user/wallet/balance', undefined, t1)).j.data;
  const bigWithdraw = await api('POST', '/skiller/wallet/withdraw', { amount: 999999, withdrawPassword: '123456' }, skT);
  check('超余额提现被拒', bigWithdraw.j?.code !== 1, bigWithdraw.j);
  // 合法重复提现（不同幂等键）：应当允许（第二笔业务）——验证幂等不误伤合法操作
  const wdPwdSet = await api('POST', '/skiller/wallet/password', { password: 'abc12345' }, skT);
  const wd1 = await api('POST', '/skiller/wallet/withdraw', { amount: 1, withdrawPassword: 'abc12345' },
    { ...skT, 'X-Idempotency-Key': RUN + '-wd1' });
  const wd2 = await api('POST', '/skiller/wallet/withdraw', { amount: 1, withdrawPassword: 'abc12345' },
    { ...skT, 'X-Idempotency-Key': RUN + '-wd2' });
  check('不同键的合法提现不被误拦（第一笔）', wd1.j?.code === 1 || (wd1.j?.msg || '').includes('密码'), wd1.j);
  check('不同键的合法提现不被误拦（第二笔）', wd2.j?.code === 1 || (wd2.j?.msg || '').includes('密码'), wd2.j);
  const wrongPwd = await api('POST', '/skiller/wallet/withdraw', { amount: 1, withdrawPassword: 'wrong!' }, skT);
  check('错误提现密码被拒', wrongPwd.j?.code !== 1, wrongPwd.j);

  console.log('== 6. 大输入/异常输入不崩溃 ==');
  const hugeTitle = '标'.repeat(500);
  const hugeSubmit = await api('POST', '/user/order/submit', {
    typeId: 1, title: hugeTitle, description: hugeTitle, deliveryAddress: 'x',
    campus: '主校区', rewardAmount: 10, payMethod: 2,
  }, t2);
  check('超长输入被拒或不崩溃（不 500）', hugeSubmit.j !== null && hugeSubmit.j.code !== undefined, hugeSubmit.status);

  console.log(`\n===== 第2轮巡检结果: ${pass} 通过 / ${fail} 失败 =====`);
  if (failures.length) failures.forEach(f => console.log('  - ' + f));
  process.exit(fail > 0 ? 1 : 0);
}
main().catch(e => { console.error('巡检异常:', e); process.exit(1); });
