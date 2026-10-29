package com.campus.runner.service;

import com.campus.runner.entity.ReconcileRecord;
import com.campus.runner.mapper.ReconcileMapper;
import com.campus.runner.mapper.ReconcileRecordMapper;
import com.campus.runner.service.impl.ReconciliationServiceImpl;
import com.campus.runner.vo.ReconcileRowVO;
import com.campus.runner.vo.ReconcileSummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 资金对账单元测试（表驱动，Mockito，不依赖数据库）
 *
 * <p>覆盖测试规范 R-01 ~ R-05：流水符号恒等式下的差异计算、
 * 一致钱包不落库、差异钱包落库、批次汇总口径、差异分类精度。
 */
class ReconciliationServiceTest {

    private ReconcileMapper reconcileMapper;
    private ReconcileRecordMapper reconcileRecordMapper;
    private ReconciliationServiceImpl service;

    @BeforeEach
    void setUp() {
        reconcileMapper = mock(ReconcileMapper.class);
        reconcileRecordMapper = mock(ReconcileRecordMapper.class);
        service = new ReconciliationServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "reconcileMapper", reconcileMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "reconcileRecordMapper", reconcileRecordMapper);
    }

    private static ReconcileRowVO row(long walletId, String balance, String expected, long txCount) {
        return ReconcileRowVO.builder()
                .walletId(walletId).userId(1000L + walletId)
                .balance(new BigDecimal(balance)).expected(new BigDecimal(expected))
                .txCount(txCount).build();
    }

    // ---------- R-01 / R-05：差异计算与精度（表驱动） ----------

    @ParameterizedTest(name = "[{index}] 余额={0} 期望={1} => 差异={2} 一致={3}")
    @CsvSource({
            //一致场景：收支成对、零流水零余额、舍入噪音内
            "'100.00', '100.00', '0.00', true",
            "'0.00',   '0.00',   '0.00', true",
            "'50.30',  '50.30',  '0.00', true",
            //差异场景：多收 / 少收 / 多扣 / 少扣（四种差异方向）
            "'120.00', '100.00', '20.00',  false",  //余额多出（少记支出或多记收入）
            "'80.00',  '100.00', '-20.00', false",  //余额少了（多记支出或漏记收入）
            "'0.00',   '30.00',  '-30.00', false",  //余额被清但流水还有 30
            "'30.00',  '0.00',   '30.00',  false",  //凭空多出的余额
            //精度边界：0.004 视为舍入噪音，0.01 起为真实差异
            "'100.00', '99.996', '0.00',  true",
            "'100.01', '100.00', '0.01',  false"
    })
    @DisplayName("差异计算表驱动：四类差异方向 + 舍入噪音容忍")
    void diffOf_tableDriven(String balance, String expected, String wantDiff, boolean wantConsistent) {
        BigDecimal diff = ReconciliationServiceImpl.diffOf(new BigDecimal(balance), new BigDecimal(expected));
        assertEquals(new BigDecimal(wantDiff), diff);
        assertEquals(wantConsistent, ReconciliationServiceImpl.isConsistent(diff));
    }

    // ---------- R-02：一致钱包不落差异记录 ----------

    @Test
    @DisplayName("一致钱包：不落差异记录，汇总全部一致")
    void reconcileAll_allMatched() {
        when(reconcileMapper.scanAll()).thenReturn(Arrays.asList(
                row(1, "200.00", "200.00", 4),
                row(2, "0.00", "0.00", 0)));
        ReconcileSummaryVO summary = service.reconcileAll();
        assertEquals(2L, summary.getCheckedWallets());
        assertEquals(0L, summary.getMismatchCount());
        assertEquals(2L, summary.getCleanCount());
        assertTrue(summary.getAllMatched());
        verify(reconcileRecordMapper, never()).insert(any());
    }

    // ---------- R-03：差异钱包落库，字段完整 ----------

    @Test
    @DisplayName("差异钱包：落一条差异记录，批次号/差异金额/流水数完整")
    void reconcileAll_mismatchRecorded() {
        when(reconcileMapper.scanAll()).thenReturn(Collections.singletonList(
                row(7, "88.00", "100.00", 12)));
        ReconcileSummaryVO summary = service.reconcileAll();
        assertEquals(1L, summary.getMismatchCount());
        assertFalse(summary.getAllMatched());
        ArgumentCaptor<ReconcileRecord> captor = ArgumentCaptor.forClass(ReconcileRecord.class);
        verify(reconcileRecordMapper).insert(captor.capture());
        ReconcileRecord rec = captor.getValue();
        assertEquals(7L, rec.getWalletId());
        assertEquals(0, new BigDecimal("-12.00").compareTo(rec.getDiffAmount()));
        assertEquals(12, rec.getTxCount());
        assertEquals(1, rec.getCheckedWallets());
        assertTrue(rec.getBatchNo().startsWith("RC"), "批次号应以 RC 前缀");
    }

    // ---------- R-04：混合批次只记差异行 ----------

    @Test
    @DisplayName("混合批次：3 修 1 差，仅差异行落库，汇总口径正确")
    void reconcileAll_mixedBatch() {
        when(reconcileMapper.scanAll()).thenReturn(Arrays.asList(
                row(1, "50.00", "50.00", 2),
                row(2, "10.00", "30.00", 5),
                row(3, "0.00", "0.00", 0),
                row(4, "77.00", "77.00", 9)));
        ReconcileSummaryVO summary = service.reconcileAll();
        assertEquals(4L, summary.getCheckedWallets());
        assertEquals(1L, summary.getMismatchCount());
        assertEquals(3L, summary.getCleanCount());
        ArgumentCaptor<ReconcileRecord> captor = ArgumentCaptor.forClass(ReconcileRecord.class);
        verify(reconcileRecordMapper).insert(captor.capture());
        assertEquals(2L, captor.getValue().getWalletId());
        assertEquals(4, captor.getValue().getCheckedWallets(), "冗余字段记录本批检查总数");
    }

    // ---------- 空库边界 ----------

    @Test
    @DisplayName("空库：无钱包时汇总为全零且不异常")
    void reconcileAll_emptyDatabase() {
        when(reconcileMapper.scanAll()).thenReturn(Collections.emptyList());
        ReconcileSummaryVO summary = service.reconcileAll();
        assertEquals(0L, summary.getCheckedWallets());
        assertTrue(summary.getAllMatched());
        List<ReconcileRecord> none = Collections.emptyList();
        verify(reconcileRecordMapper, never()).insert(any());
    }
}
