package com.campus.runner.it;

import com.campus.runner.dto.OrdersGrabDTO;
import com.campus.runner.dto.OrdersSubmitDTO;
import com.campus.runner.entity.Orders;
import com.campus.runner.entity.Runner;
import com.campus.runner.exception.GrabFailedException;
import com.campus.runner.exception.InsufficientBalanceException;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.service.OrderService;
import com.campus.runner.service.WalletService;
import com.campus.runner.vo.OrderSubmitVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资金与抢单并发对账集成测试（真实数据库 + Redis，profile=test）
 *
 * <p>覆盖毕业设计"系统测试"章节的两类并发正确性：
 * 1. 抢单竞态：N 个技能者并发抢同一单，恰好 1 人成功，不双抢、不丢单；
 * 2. 钱包对账：并发充值与并发扣款后，余额与成功流水严格一致，余额永不透支。
 *
 * <p>运行前提：本地 MySQL（campus_runner_test 库，结构同 campus_runner）与 Redis 已启动：
 * mvn test -Dtest=GrabWalletConcurrencyIT -Dspring-boot.run.profiles=test
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GrabWalletConcurrencyIT {

    @Autowired
    private OrderService orderService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private RunnerMapper runnerMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String tag = "it" + System.currentTimeMillis();

    private long insertUser(String name) {
        String openid = tag + "_" + name;
        jdbcTemplate.update("insert into user (openid, name, credit_score, publish_order_count, create_time) values (?, ?, 100, 0, now())",
                openid, name);
        return jdbcTemplate.queryForObject("select id from user where openid = ?", Long.class, openid);
    }

    /** 直接入库构造认证通过的技能者（绕开 HTTP 入驻审核流程） */
    private long insertCertifiedSkiller(int i) {
        long userId = insertUser("压测技能者" + i);
        Runner runner = Runner.builder()
                .userId(userId).name("压测技能者" + i).phone("1380000" + String.format("%04d", i))
                .studentNo("IT" + System.currentTimeMillis() + i).campus("主校区").college("计算机学院")
                .auditStatus(1).runnerLevel(1).dailyOrderLimit(1000).completedOrders(0)
                .score(new BigDecimal("4.8")).status(1)
                .createTime(LocalDateTime.now()).updateTime(LocalDateTime.now())
                .build();
        runnerMapper.insert(runner);
        return runner.getId();
    }

    /** 发布并支付一单悬赏，返回订单 id（状态=待接单） */
    private long createPaidOrder(long publisherId) {
        walletService.recharge(publisherId, new BigDecimal("1000"));
        OrdersSubmitDTO dto = new OrdersSubmitDTO();
        dto.setTypeId(1L);
        dto.setTitle("并发测试订单-" + publisherId);
        dto.setDescription("IT 自动生成");
        dto.setDeliveryAddress("测试楼1");
        dto.setCampus("主校区");
        dto.setRewardAmount(new BigDecimal("50"));
        dto.setPayMethod(2);
        OrderSubmitVO vo = orderService.submit(publisherId, dto);
        orderService.pay(vo.getOrderNumber(), publisherId);
        return vo.getId();
    }

    @Test
    @DisplayName("抢单竞态：30 名技能者并发抢同一单，恰好 1 人成功且落库一致")
    void grabRace_exactlyOneWinner() throws Exception {
        long publisher = insertUser("发单用户");
        long orderId = createPaidOrder(publisher);

        int n = 30;
        List<Long> skillers = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            skillers.add(insertCertifiedSkiller(i));
        }

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<Long> winner = new java.util.concurrent.atomic.AtomicReference<>();

        for (Long sk : skillers) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    orderService.grab(sk, new OrdersGrabDTO(orderId));
                    success.incrementAndGet();
                    winner.set(sk);
                } catch (GrabFailedException ignored) {
                    //竞争失败属预期
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "抢单线程未在限时内结束");

        assertEquals(1, success.get(), "并发抢单应恰好 1 人成功");
        Map<String, Object> order = jdbcTemplate.queryForMap("select status, runner_id from orders where id = ?", orderId);
        assertEquals(3, ((Number) order.get("status")).intValue(), "订单应进入进行中(状态3)");
        assertEquals(winner.get(), ((Number) order.get("runner_id")).longValue(), "落库接单人应与成功线程一致");
    }

    @Test
    @DisplayName("钱包对账：并发充值后并发扣款，余额与成功流水严格一致且不透支")
    void walletConcurrent_reconcile() throws Exception {
        long user = insertUser("钱包用户");

        //并发充值：20 线程 × 10 元
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger rechargeFail = new AtomicInteger();
        java.util.List<String> rechargeErrors = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    walletService.recharge(user, new BigDecimal("10"));
                } catch (Exception e) {
                    rechargeFail.incrementAndGet();
                    rechargeErrors.add(String.valueOf(e.getMessage()));
                }
            });
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertTrue(rechargeErrors.isEmpty(), "并发充值不应失败: " + rechargeErrors);
        BigDecimal afterRecharge = jdbcTemplate.queryForObject(
                "select balance from wallet_account where user_id = ?", BigDecimal.class, user);
        assertEquals(0, new BigDecimal("200").compareTo(afterRecharge), "并发充值后余额应为 200");

        //并发扣款：20 线程 × 15 元（总额 300 > 200，部分应因余额不足被拒）
        ExecutorService pool2 = Executors.newFixedThreadPool(threads);
        CountDownLatch ready2 = new CountDownLatch(threads);
        CountDownLatch start2 = new CountDownLatch(1);
        AtomicInteger deductOk = new AtomicInteger();
        AtomicInteger deductRejected = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            pool2.submit(() -> {
                ready2.countDown();
                try {
                    start2.await();
                    walletService.changeBalance(user, new BigDecimal("-15"), 2, null, "并发扣款");
                    deductOk.incrementAndGet();
                } catch (InsufficientBalanceException e) {
                    deductRejected.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        assertTrue(ready2.await(10, TimeUnit.SECONDS));
        start2.countDown();
        pool2.shutdown();
        assertTrue(pool2.awaitTermination(30, TimeUnit.SECONDS));

        BigDecimal finalBalance = jdbcTemplate.queryForObject(
                "select balance from wallet_account where user_id = ?", BigDecimal.class, user);
        assertEquals(0, new BigDecimal(200 - deductOk.get() * 15).compareTo(finalBalance),
                "余额必须等于 200 - 成功扣款总额（与流水严格对账）");
        assertTrue(finalBalance.compareTo(BigDecimal.ZERO) >= 0, "余额永不透支");
        assertEquals(threads, deductOk.get() + deductRejected.get(), "每笔扣款要么成功要么明确拒绝");
        assertEquals(13, deductOk.get(), "200 元最多支持 13 笔 15 元扣款");
    }
}
