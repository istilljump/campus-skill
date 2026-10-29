package com.campus.runner.it;

import com.campus.runner.entity.ReconcileRecord;
import com.campus.runner.mapper.ReconcileRecordMapper;
import com.campus.runner.service.ReconciliationService;
import com.campus.runner.vo.ReconcileSummaryVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资金对账闭环集成测试（真实数据库，profile=test）
 *
 * <p>覆盖测试规范 R-06：Jdbc 构造一致/差异两类钱包 → 执行对账 →
 * 差异钱包落 reconcile_record（字段正确），一致钱包不落库。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ReconcileIT {

    @Autowired
    private ReconciliationService reconciliationService;
    @Autowired
    private ReconcileRecordMapper reconcileRecordMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String tag = "rc" + System.currentTimeMillis();

    private long insertUserWithWallet(String name, String balance) {
        String openid = tag + "_" + name;
        jdbcTemplate.update("insert into user (openid, name, credit_score, publish_order_count, create_time) values (?, ?, 100, 0, now())",
                openid, name);
        long userId = jdbcTemplate.queryForObject("select id from user where openid = ?", Long.class, openid);
        jdbcTemplate.update("insert into wallet_account (user_id, balance, frozen_amount, total_income, total_expense, status, create_time, update_time) " +
                "values (?, ?, 0, 0, 0, 1, now(), now())", userId, balance);
        return userId;
    }

    private long walletIdOf(long userId) {
        return jdbcTemplate.queryForObject("select id from wallet_account where user_id = ?", Long.class, userId);
    }

    @Test
    @DisplayName("R-06 对账闭环：差异钱包落库、一致钱包放行、汇总口径正确")
    void reconcileEndToEnd() {
        //1. 一致钱包：充值 50（type 3）+ 支出 20（type 2）→ 期望 30，余额 30
        long consistentUser = insertUserWithWallet("一致钱包", "30.00");
        long consistentWallet = walletIdOf(consistentUser);
        jdbcTemplate.update("insert into wallet_transaction (wallet_id, type, amount, remark, create_time) values (?, 3, 50.00, '充值', now())", consistentWallet);
        jdbcTemplate.update("insert into wallet_transaction (wallet_id, type, amount, remark, create_time) values (?, 2, 20.00, '支付', now())", consistentWallet);

        //2. 差异钱包：流水净额 70（充值 100 - 提现 30），余额却只有 40 → 差异 -30.00
        long mismatchUser = insertUserWithWallet("差异钱包", "40.00");
        long mismatchWallet = walletIdOf(mismatchUser);
        jdbcTemplate.update("insert into wallet_transaction (wallet_id, type, amount, remark, create_time) values (?, 3, 100.00, '充值', now())", mismatchWallet);
        jdbcTemplate.update("insert into wallet_transaction (wallet_id, type, amount, remark, create_time) values (?, 4, 30.00, '提现', now())", mismatchWallet);

        ReconcileSummaryVO summary = reconciliationService.reconcileAll();
        assertTrue(summary.getCheckedWallets() >= 2, "至少检查本用例的两个钱包");
        assertTrue(summary.getMismatchCount() >= 1, "应发现至少 1 个差异钱包");

        String batchNo = summary.getBatchNo();
        List<ReconcileRecord> batchRecords = reconcileRecordMapper.listByBatch(batchNo);
        ReconcileRecord hit = batchRecords.stream()
                .filter(r -> r.getWalletId() == mismatchWallet)
                .findFirst().orElse(null);
        assertNotNull(hit, "差异钱包应落对账记录");
        assertEquals(0, new BigDecimal("-30.00").compareTo(hit.getDiffAmount()), "差异金额应为 -30.00（余额40 - 期望70）");
        assertEquals(0, new BigDecimal("40.00").compareTo(hit.getBalance()));
        assertEquals(0, new BigDecimal("70.00").compareTo(hit.getExpectedBalance()));
        assertEquals(2, hit.getTxCount());
        assertTrue(batchRecords.stream().noneMatch(r -> r.getWalletId() == consistentWallet), "一致钱包不应落差异记录");
    }
}
