package com.campus.runner.service.risk.impl;

import com.campus.runner.mapper.RiskControlLogMapper;
import com.campus.runner.service.risk.RiskEvent;
import com.campus.runner.service.risk.RiskRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * R004 返修率异常：技能者最近 10 单中返修 ≥ 3 单标记中等风险（交付质量劣化信号），
 * 提示管理端关注，可与信用分引擎（返修-3）形成"自动扣分 + 人工介入"的双层治理。
 */
@Component
public class ReworkRateRule implements RiskRule {

    public static final int WINDOW_ORDERS = 10;
    public static final int THRESHOLD = 3;
    public static final int LEVEL_MEDIUM = 2;

    @Autowired
    private RiskControlLogMapper riskControlLogMapper;

    @Override
    public String code() {
        return "R004";
    }

    @Override
    public String name() {
        return "返修率异常";
    }

    @Override
    public Set<RiskEvent.TriggerType> supportedTriggers() {
        return Collections.singleton(RiskEvent.TriggerType.REWORK);
    }

    @Override
    public RiskFinding evaluate(RiskEvent event) {
        int reworks = riskControlLogMapper.countRecentReworks(event.getSkillerId(), WINDOW_ORDERS);
        if (reworks < THRESHOLD) {
            return null;
        }
        return RiskFinding.builder()
                .level(LEVEL_MEDIUM)
                .targetType(RiskFinding.TARGET_SKILLER)
                .targetId(event.getSkillerId())
                .orderId(event.getOrderId())
                .detail("最近 " + WINDOW_ORDERS + " 单中返修 " + reworks + " 单，达到阈值 " + THRESHOLD + "，交付质量劣化")
                .build();
    }
}
