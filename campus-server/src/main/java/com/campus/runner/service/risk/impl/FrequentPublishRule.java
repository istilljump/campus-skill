package com.campus.runner.service.risk.impl;

import com.campus.runner.mapper.RiskControlLogMapper;
import com.campus.runner.service.risk.RiskEvent;
import com.campus.runner.service.risk.RiskRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * R001 高频发单：10 分钟窗口内发单 ≥ 5 单判定为刷单/恶意占用大厅风险，等级高。
 * 阈值口径与抢单限流（10 秒 5 次）一致采用"短窗口 + 计数"模式。
 */
@Component
public class FrequentPublishRule implements RiskRule {

    public static final int WINDOW_MINUTES = 10;
    public static final int THRESHOLD = 5;
    public static final int LEVEL_HIGH = 3;

    @Autowired
    private RiskControlLogMapper riskControlLogMapper;

    @Override
    public String code() {
        return "R001";
    }

    @Override
    public String name() {
        return "高频发单";
    }

    @Override
    public Set<RiskEvent.TriggerType> supportedTriggers() {
        return Collections.singleton(RiskEvent.TriggerType.PUBLISH);
    }

    @Override
    public RiskFinding evaluate(RiskEvent event) {
        int recent = riskControlLogMapper.countRecentOrders(event.getUserId(), WINDOW_MINUTES);
        if (recent < THRESHOLD) {
            return null;
        }
        return RiskFinding.builder()
                .level(LEVEL_HIGH)
                .targetType(RiskFinding.TARGET_USER)
                .targetId(event.getUserId())
                .orderId(event.getOrderId())
                .detail(WINDOW_MINUTES + "分钟内发单 " + recent + " 单，超过阈值 " + THRESHOLD + "，疑似刷单占用大厅")
                .build();
    }
}
