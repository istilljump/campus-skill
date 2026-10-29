package com.campus.runner.service.risk.impl;

import com.campus.runner.service.risk.RiskEvent;
import com.campus.runner.service.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

/**
 * R002 大额提现：单笔提现 ≥ 500 元标记中等风险（洗钱/盗号快速变现特征），
 * 仅告警不打断——提现本身仍有密码 + 管理端人工审核两道闸。
 */
@Component
public class LargeWithdrawRule implements RiskRule {

    public static final BigDecimal THRESHOLD = new BigDecimal("500");
    public static final int LEVEL_MEDIUM = 2;

    @Override
    public String code() {
        return "R002";
    }

    @Override
    public String name() {
        return "大额提现";
    }

    @Override
    public Set<RiskEvent.TriggerType> supportedTriggers() {
        return Collections.singleton(RiskEvent.TriggerType.WITHDRAW);
    }

    @Override
    public RiskFinding evaluate(RiskEvent event) {
        if (event.getAmount() == null || event.getAmount().compareTo(THRESHOLD) < 0) {
            return null;
        }
        return RiskFinding.builder()
                .level(LEVEL_MEDIUM)
                .targetType(RiskFinding.TARGET_USER)
                .targetId(event.getUserId())
                .detail("单笔提现 ¥" + event.getAmount() + "，达到大额阈值 ¥" + THRESHOLD + "，请人工复核提现审核")
                .build();
    }
}
