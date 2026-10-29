package com.campus.runner.it;

import com.campus.runner.controller.user.OrderController;
import com.campus.runner.controller.user.WalletController;
import com.campus.runner.dto.OrdersSubmitDTO;
import com.campus.runner.dto.WalletRechargeDTO;
import com.campus.runner.exception.BusinessException;
import com.campus.runner.service.OrderService;
import com.campus.runner.service.WalletService;
import com.campus.runner.vo.OrderSubmitVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资金接口幂等并发集成测试（真实数据库 + Redis，profile=test）
 *
 * <p>覆盖测试规范 I-01 ~ I-04：同一幂等键的并发充值/支付恰好成功一次、
 * 首笔失败后同键重试放行、未携带键的请求不启用幂等。
 *
 * <p>通过 AOP 代理直接调用控制器方法（幂等切面在控制器层生效），
 * 每线程注入 Mock 请求上下文携带相同的 X-Idempotency-Key。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IdempotencyIT {

    @Autowired
    private WalletController walletController;
    @Autowired
    private OrderController orderController;
    @Autowired
    private OrderService orderService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String tag = "idem" + System.currentTimeMillis();

    @AfterEach
    void cleanThreadContext() {
        BaseContextCleaner.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    /** 保证 BaseContext ThreadLocal 在线程池场景下被清理 */
    private static final class BaseContextCleaner {
        static void clear() {
            com.campus.runner.context.BaseContext.removeCurrentId();
        }
    }

    private long insertUser(String name) {
        String openid = tag + "_" + name;
        jdbcTemplate.update("insert into user (openid, name, credit_score, publish_order_count, create_time) values (?, ?, 100, 0, now())",
                openid, name);
        return jdbcTemplate.queryForObject("select id from user where openid = ?", Long.class, openid);
    }

    private BigDecimal balanceOf(long userId) {
        return jdbcTemplate.queryForObject("select balance from wallet_account where user_id = ?", BigDecimal.class, userId);
    }

    /**
     * 为当前线程注入登录上下文与带幂等键的请求上下文
     */
    private void bindContext(long userId, String idemKey) {
        com.campus.runner.context.BaseContext.setCurrentId(userId);
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (idemKey != null) {
            request.addHeader(com.campus.runner.aspect.IdempotentAspect.HEADER_KEY, idemKey);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    /** N 线程携带同一幂等键并发执行 action，返回成功/失败计数与失败消息样本 */
    private int[] fireConcurrently(int n, long userId, String idemKey, Runnable action,
                                   AtomicInteger failCount, List<String> failSamples) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    bindContext(userId, idemKey);
                    action.run();
                    ok.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                    synchronized (failSamples) {
                        if (failSamples.size() < 3) {
                            failSamples.add(String.valueOf(e.getMessage()));
                        }
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    synchronized (failSamples) {
                        if (failSamples.size() < 3) {
                            failSamples.add("UNEXPECTED:" + e);
                        }
                    }
                } finally {
                    com.campus.runner.context.BaseContext.removeCurrentId();
                    RequestContextHolder.resetRequestAttributes();
                }
            });
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "线程未在限时内结束");
        return new int[]{ok.get()};
    }

    @Test
    @DisplayName("I-01 同一幂等键 20 线程并发充值：恰好成功 1 次，余额只加一次")
    void recharge_sameKeyExactlyOnce() throws Exception {
        long user = insertUser("充值用户");
        String key = tag + "-recharge";

        AtomicInteger failCount = new AtomicInteger();
        List<String> failSamples = new java.util.concurrent.CopyOnWriteArrayList<>();
        int[] ok = fireConcurrently(20, user, key, () ->
                walletController.recharge(WalletRechargeDTO.builder().amount(new BigDecimal("100")).build()),
                failCount, failSamples);

        assertEquals(1, ok[0], "同键并发充值应恰好成功 1 次");
        assertEquals(19, failCount.get(), "其余 19 次应被幂等拦截: " + failSamples);
        assertEquals(0, new BigDecimal("100").compareTo(balanceOf(user)), "余额只应加一次 100");
    }

    @Test
    @DisplayName("I-02 同一幂等键 20 线程并发支付同一订单：恰好扣款 1 次")
    void pay_sameKeyExactlyOnce() throws Exception {
        long user = insertUser("支付用户");
        walletService.recharge(user, new BigDecimal("1000"));
        OrdersSubmitDTO dto = new OrdersSubmitDTO();
        dto.setTypeId(1L);
        dto.setTitle("幂等支付测试订单");
        dto.setDescription("IT 自动生成");
        dto.setDeliveryAddress("测试楼2");
        dto.setCampus("主校区");
        dto.setRewardAmount(new BigDecimal("50"));
        dto.setPayMethod(2);
        bindContext(user, null);
        OrderSubmitVO vo = orderService.submit(user, dto);
        String key = tag + "-pay-" + vo.getOrderNumber();

        AtomicInteger failCount = new AtomicInteger();
        List<String> failSamples = new java.util.concurrent.CopyOnWriteArrayList<>();
        int[] ok = fireConcurrently(20, user, key, () ->
                        orderController.pay(vo.getOrderNumber()),
                failCount, failSamples);

        assertEquals(1, ok[0], "同键并发支付应恰好成功 1 次");
        assertEquals(19, failCount.get(), "其余 19 次应被幂等拦截: " + failSamples);
        assertEquals(0, new BigDecimal("950").compareTo(balanceOf(user)), "1000-50 应只扣一次");
        Map<String, Object> order = jdbcTemplate.queryForMap("select pay_status, status from orders where id = ?", vo.getId());
        assertEquals(1, ((Number) order.get("pay_status")).intValue(), "订单应已支付");
        assertEquals(2, ((Number) order.get("status")).intValue(), "悬赏单支付后进入待接单");
    }

    @Test
    @DisplayName("I-03 首笔业务失败释放幂等键：同键重试可成功")
    void pay_failedFirstAttempt_releasesKey() {
        long user = insertUser("重试用户");
        OrdersSubmitDTO dto = new OrdersSubmitDTO();
        dto.setTypeId(1L);
        dto.setTitle("幂等重试测试订单");
        dto.setDeliveryAddress("测试楼3");
        dto.setCampus("主校区");
        dto.setRewardAmount(new BigDecimal("30"));
        dto.setPayMethod(2);
        bindContext(user, null);
        OrderSubmitVO vo = orderService.submit(user, dto);

        String key = tag + "-retry";
        bindContext(user, key);
        //首笔：余额为 0，支付业务失败
        assertThrows(Exception.class, () -> orderController.pay(vo.getOrderNumber()));

        //同键重试：充值后应放行成功
        walletService.recharge(user, new BigDecimal("100"));
        orderController.pay(vo.getOrderNumber());
        assertEquals(0, new BigDecimal("70").compareTo(balanceOf(user)), "重试成功后余额应为 100-30");
    }

    @Test
    @DisplayName("I-04 未携带幂等键：直接放行不启用幂等（兼容旧客户端）")
    void recharge_noKeyPassthrough() {
        long user = insertUser("无键用户");
        bindContext(user, null);
        walletController.recharge(WalletRechargeDTO.builder().amount(new BigDecimal("10")).build());
        walletController.recharge(WalletRechargeDTO.builder().amount(new BigDecimal("10")).build());
        assertEquals(0, new BigDecimal("20").compareTo(balanceOf(user)), "未带键的两次请求都应执行");
    }
}
