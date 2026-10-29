package com.campus.runner.service.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 风控事件：业务关键节点（发单/提现/返修）触发规则引擎的统一入参。
 * 事件本身不可变，规则只读事件、只产出结论（RiskFinding），不落库——落库统一由引擎负责。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEvent {

    /**
     * 事件触发类型
     */
    public enum TriggerType {
        /** 发单 */
        PUBLISH,
        /** 提现申请 */
        WITHDRAW,
        /** 订单返修 */
        REWORK
    }

    private TriggerType type;

    //发单/提现用户 id
    private Long userId;

    //技能者 id（REWORK 事件）
    private Long skillerId;

    //关联订单 id
    private Long orderId;

    //金额（提现事件）
    private BigDecimal amount;

    public static RiskEvent publish(Long userId, Long orderId) {
        return RiskEvent.builder().type(TriggerType.PUBLISH).userId(userId).orderId(orderId).build();
    }

    public static RiskEvent withdraw(Long userId, BigDecimal amount) {
        return RiskEvent.builder().type(TriggerType.WITHDRAW).userId(userId).amount(amount).build();
    }

    public static RiskEvent rework(Long skillerId, Long orderId) {
        return RiskEvent.builder().type(TriggerType.REWORK).skillerId(skillerId).orderId(orderId).build();
    }
}
