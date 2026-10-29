package com.campus.runner.service.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * 风控规则（策略模式）：每条规则一个 Spring Bean，引擎按事件类型路由到支持的规则。
 * 规则只做「判断」，不落库不通知——统一由引擎处理，保证规则可独立测试、可插拔。
 */
public interface RiskRule {

    /**
     * 规则编码（唯一），如 R001
     */
    String code();

    /**
     * 规则名称，如：高频发单
     */
    String name();

    /**
     * 支持的事件类型
     */
    Set<RiskEvent.TriggerType> supportedTriggers();

    /**
     * 评估事件，命中返回结论（可为多条），未命中返回空列表。
     * 实现必须只依赖入参与 Mapper 查询，不得抛出业务异常中断主流程（引擎另有兜底）。
     */
    RiskFinding evaluate(RiskEvent event);

    /**
     * 单条命中结论
     */
    @Data
    @Builder
    @AllArgsConstructor
    class RiskFinding {
        public static final int TARGET_USER = 1;
        public static final int TARGET_SKILLER = 2;

        //风险等级 1低 2中 3高
        private int level;
        //1用户 2技能者
        private int targetType;
        private Long targetId;
        private Long orderId;
        private String detail;
    }
}
