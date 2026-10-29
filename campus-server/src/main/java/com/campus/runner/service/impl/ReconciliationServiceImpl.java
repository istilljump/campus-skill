package com.campus.runner.service.impl;

import com.campus.runner.entity.ReconcileRecord;
import com.campus.runner.mapper.ReconcileMapper;
import com.campus.runner.mapper.ReconcileRecordMapper;
import com.campus.runner.service.ReconciliationService;
import com.campus.runner.vo.ReconcileRowVO;
import com.campus.runner.vo.ReconcileSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ReconciliationServiceImpl implements ReconciliationService {

    /**
     * 差异容忍度：金额均保留两位小数，0.005 以内视为舍入噪音
     */
    public static final BigDecimal TOLERANCE = new BigDecimal("0.005");

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private ReconcileMapper reconcileMapper;

    @Autowired
    private ReconcileRecordMapper reconcileRecordMapper;

    /**
     * 对账核心纯函数：差异 = 当前余额 - 期望余额。
     * 收入类流水（1收入/3充值/5退款）记正、支出类（2支付/4提现/6违约金）记负，
     * 恒等式成立时差异恒为 0。
     */
    public static BigDecimal diffOf(BigDecimal balance, BigDecimal expected) {
        BigDecimal b = balance == null ? BigDecimal.ZERO : balance;
        BigDecimal e = expected == null ? BigDecimal.ZERO : expected;
        return b.subtract(e).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 对账结论：差异绝对值在容忍度内视为一致（舍入噪音不告警）
     */
    public static boolean isConsistent(BigDecimal diff) {
        return diff == null || diff.abs().compareTo(TOLERANCE) < 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconcileSummaryVO reconcileAll() {
        String batchNo = "RC" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", SECURE_RANDOM.nextInt(1000));
        List<ReconcileRowVO> rows = reconcileMapper.scanAll();
        long mismatch = 0;
        for (ReconcileRowVO row : rows) {
            BigDecimal diff = diffOf(row.getBalance(), row.getExpected());
            if (isConsistent(diff)) {
                continue;
            }
            mismatch++;
            ReconcileRecord record = ReconcileRecord.builder()
                    .batchNo(batchNo)
                    .walletId(row.getWalletId())
                    .userId(row.getUserId())
                    .balance(row.getBalance())
                    .expectedBalance(row.getExpected())
                    .diffAmount(diff)
                    .txCount(row.getTxCount() == null ? 0 : row.getTxCount().intValue())
                    .checkedWallets(rows.size())
                    .createTime(LocalDateTime.now())
                    .build();
            reconcileRecordMapper.insert(record);
            log.error("[资金对账]发现差异！batchNo={}, walletId={}, userId={}, 余额={}, 期望={}, 差异={}, 流水笔数={}",
                    batchNo, row.getWalletId(), row.getUserId(), row.getBalance(), row.getExpected(), diff, row.getTxCount());
        }
        //批次汇总落库（含 0 差异轮）："最近批次"语义不因该轮干净而丢失
        reconcileRecordMapper.insertBatch(batchNo, rows.size(), (int) mismatch, mismatch == 0, LocalDateTime.now());
        ReconcileSummaryVO summary = ReconcileSummaryVO.builder()
                .batchNo(batchNo)
                .checkedWallets((long) rows.size())
                .mismatchCount(mismatch)
                .cleanCount(rows.size() - mismatch)
                .allMatched(mismatch == 0)
                .createTime(LocalDateTime.now())
                .build();
        if (mismatch == 0) {
            log.info("[资金对账]批次 {} 完成：检查 {} 个钱包，全部一致", batchNo, rows.size());
        } else {
            log.warn("[资金对账]批次 {} 完成：检查 {} 个钱包，差异 {} 个，已落库并告警", batchNo, rows.size(), mismatch);
        }
        return summary;
    }
}
