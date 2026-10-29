package com.campus.runner.service.risk.impl;

import com.campus.runner.mapper.RiskControlLogMapper;
import com.campus.runner.service.risk.RiskEvent;
import com.campus.runner.service.risk.RiskRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * R003 高频提现：当日提现申请 ≥ 3 笔标记中等风险（频繁进出账异常特征）。
 */
@Component
public class FrequentWithdrawRule implements RiskRule {

    public static final int THRESHOLD = 3;
    public static final int LEVEL_MEDIUM = 2;

    @Autowired
    private RiskControlLogMapper riskControlLogMapper;

    @Override
    public String code() {
        return "R003";
    }

    @Override
    public String name() {
        return "高频提现";
    }

    @Override
    public Set<RiskEvent.TriggerType> supportedTriggers() {
        return Collections.singleton(RiskEvent.TriggerType.WITHDRAW);
    }

    @Override
    public RiskFinding evaluate(RiskEvent event) {
        int today = riskControlLogMapper.countTodayWithdraw(event.getUserId());
        if (today < THRESHOLD) {
            return null;
        }
        return RiskFinding.builder()
                .level(LEVEL_MEDIUM)
                .targetType(RiskFinding.TARGET_USER)
                .targetId(event.getUserId())
                .detail("当日提现申请 " + today + " 笔，超过阈值 " + THRESHOLD + "，存在频繁进出账特征")
                .build();
    }
}
