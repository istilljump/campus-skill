package com.campus.runner.service;

import com.campus.runner.mapper.RiskControlLogMapper;
import com.campus.runner.service.impl.RiskControlServiceImpl;
import com.campus.runner.service.risk.RiskEvent;
import com.campus.runner.service.risk.RiskRule;
import com.campus.runner.service.risk.impl.FrequentPublishRule;
import com.campus.runner.service.risk.impl.FrequentWithdrawRule;
import com.campus.runner.service.risk.impl.LargeWithdrawRule;
import com.campus.runner.service.risk.impl.ReworkRateRule;
import com.campus.runner.entity.RiskControlLog;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 风控规则引擎单元测试（表驱动，Mockito，不依赖数据库）
 *
 * <p>覆盖测试规范 K-01 ~ K-05：四条规则的阈值边界表、
 * 规则故障隔离（单条规则异常不影响其余规则与主流程）。
 */
class RiskControlServiceTest {

    private RiskControlLogMapper logMapper;
    private FrequentPublishRule publishRule;
    private LargeWithdrawRule largeWithdrawRule;
    private FrequentWithdrawRule frequentWithdrawRule;
    private ReworkRateRule reworkRateRule;
    private RiskControlServiceImpl engine;

    @BeforeEach
    void setUp() {
        logMapper = mock(RiskControlLogMapper.class);
        publishRule = new FrequentPublishRule();
        org.springframework.test.util.ReflectionTestUtils.setField(publishRule, "riskControlLogMapper", logMapper);
        frequentWithdrawRule = new FrequentWithdrawRule();
        org.springframework.test.util.ReflectionTestUtils.setField(frequentWithdrawRule, "riskControlLogMapper", logMapper);
        reworkRateRule = new ReworkRateRule();
        org.springframework.test.util.ReflectionTestUtils.setField(reworkRateRule, "riskControlLogMapper", logMapper);
        largeWithdrawRule = new LargeWithdrawRule();
        engine = new RiskControlServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(engine, "rules",
                Arrays.asList(publishRule, largeWithdrawRule, frequentWithdrawRule, reworkRateRule));
        org.springframework.test.util.ReflectionTestUtils.setField(engine, "riskControlLogMapper", logMapper);
    }

    // ---------- K-01：R001 高频发单阈值表 ----------

    @ParameterizedTest(name = "[{index}] 10分钟发单 {0} 单 => 命中={1}")
    @CsvSource({
            "0, false", "1, false", "4, false", "5, true", "6, true", "20, true"
    })
    @DisplayName("K-01 R001 高频发单：阈值 5 的边界表")
    void frequentPublish_thresholdTable(int recentOrders, boolean expectHit) {
        when(logMapper.countRecentOrders(100L, FrequentPublishRule.WINDOW_MINUTES)).thenReturn(recentOrders);
        RiskRule.RiskFinding finding = publishRule.evaluate(RiskEvent.publish(100L, 900L));
        if (expectHit) {
            assertNotNull(finding);
            assertEquals(3, finding.getLevel(), "刷单风险应为高级别");
            assertEquals(1, finding.getTargetType());
            assertEquals(100L, finding.getTargetId());
            assertTrue(finding.getDetail().contains(String.valueOf(recentOrders)), "详情应包含实际单数");
        } else {
            assertNull(finding);
        }
    }

    // ---------- K-02：R002 大额提现边界表 ----------

    @ParameterizedTest(name = "[{index}] 提现 {0} 元 => 命中={1}")
    @CsvSource({
            "100.00, false", "499.99, false", "500.00, true", "501.50, true", "5000.00, true"
    })
    @DisplayName("K-02 R002 大额提现：500 元为命中边界")
    void largeWithdraw_boundaryTable(String amount, boolean expectHit) {
        RiskRule.RiskFinding finding = largeWithdrawRule.evaluate(RiskEvent.withdraw(100L, new BigDecimal(amount)));
        if (expectHit) {
            assertNotNull(finding);
            assertEquals(2, finding.getLevel());
            assertTrue(finding.getDetail().contains(amount));
        } else {
            assertNull(finding);
        }
    }

    // ---------- K-03：R003 高频提现边界表 ----------

    @ParameterizedTest(name = "[{index}] 当日提现 {0} 笔 => 命中={1}")
    @CsvSource({
            "0, false", "1, false", "2, false", "3, true", "4, true"
    })
    @DisplayName("K-03 R003 高频提现：当日第 3 笔命中")
    void frequentWithdraw_thresholdTable(int todayCount, boolean expectHit) {
        when(logMapper.countTodayWithdraw(100L)).thenReturn(todayCount);
        RiskRule.RiskFinding finding = frequentWithdrawRule.evaluate(RiskEvent.withdraw(100L, new BigDecimal("50")));
        if (expectHit) {
            assertNotNull(finding);
            assertEquals(2, finding.getLevel());
        } else {
            assertNull(finding);
        }
    }

    // ---------- K-04：R004 返修率边界表 ----------

    @ParameterizedTest(name = "[{index}] 近10单返修 {0} 单 => 命中={1}")
    @CsvSource({
            "0, false", "1, false", "2, false", "3, true", "4, true", "10, true"
    })
    @DisplayName("K-04 R004 返修率异常：近10单返修≥3命中")
    void reworkRate_thresholdTable(int reworks, boolean expectHit) {
        when(logMapper.countRecentReworks(20L, ReworkRateRule.WINDOW_ORDERS)).thenReturn(reworks);
        RiskRule.RiskFinding finding = reworkRateRule.evaluate(RiskEvent.rework(20L, 800L));
        if (expectHit) {
            assertNotNull(finding);
            assertEquals(2, finding.getLevel());
            assertEquals(2, finding.getTargetType(), "返修规则对象应为技能者");
            assertEquals(20L, finding.getTargetId());
        } else {
            assertNull(finding);
        }
    }

    // ---------- 引擎路由与落库 ----------

    @Test
    @DisplayName("引擎按事件类型路由：WITHDRAW 事件不触发发单/返修规则")
    void engine_routesByTriggerType() {
        when(logMapper.countTodayWithdraw(100L)).thenReturn(5);
        engine.fire(RiskEvent.withdraw(100L, new BigDecimal("600")));
        ArgumentCaptor<RiskControlLog> captor = ArgumentCaptor.forClass(RiskControlLog.class);
        verify(logMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        List<RiskControlLog> inserted = captor.getAllValues();
        assertEquals(2, inserted.size(), "大额 + 高频两条规则应命中");
        assertTrue(inserted.stream().allMatch(l -> "R002".equals(l.getRuleCode()) || "R003".equals(l.getRuleCode())));
    }

    @Test
    @DisplayName("K-05 规则故障隔离：单条规则抛异常，其余规则照常执行")
    void engine_isolatesFailingRule() {
        when(logMapper.countTodayWithdraw(100L)).thenReturn(5);
        RiskRule broken = mock(RiskRule.class);
        when(broken.supportedTriggers()).thenReturn(Collections.singleton(RiskEvent.TriggerType.WITHDRAW));
        when(broken.evaluate(any())).thenThrow(new RuntimeException("规则炸了"));
        RiskControlServiceImpl withBroken = new RiskControlServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(withBroken, "rules",
                Arrays.asList(broken, largeWithdrawRule));
        org.springframework.test.util.ReflectionTestUtils.setField(withBroken, "riskControlLogMapper", logMapper);

        withBroken.fire(RiskEvent.withdraw(100L, new BigDecimal("600")));
        ArgumentCaptor<RiskControlLog> captor = ArgumentCaptor.forClass(RiskControlLog.class);
        verify(logMapper).insert(captor.capture());
        assertEquals("R002", captor.getValue().getRuleCode(), "故障规则之后的规则应照常落库");
    }

    @Test
    @DisplayName("未命中：不落库不告警")
    void engine_noHitNoInsert() {
        when(logMapper.countRecentOrders(any(), anyInt())).thenReturn(1);
        engine.fire(RiskEvent.publish(100L, 900L));
        verify(logMapper, never()).insert(any(RiskControlLog.class));
    }
}
