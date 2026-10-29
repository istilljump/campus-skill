package com.campus.runner.service;

import com.campus.runner.entity.RiskControlLog;
import com.campus.runner.result.PageResult;
import com.campus.runner.service.risk.RiskEvent;

/**
 * 风控引擎：业务关键节点异步触发，规则命中落库 risk_control_log，绝不阻断主流程。
 */
public interface RiskControlService {

    /**
     * 触发规则评估（异步执行，任何规则异常都被吞掉并告警日志）
     */
    void fire(RiskEvent event);

    /**
     * 风控告警分页
     */
    PageResult<RiskControlLog> page(Integer status, Integer level, Integer page, Integer pageSize);

    /**
     * 处置告警（仅未处置的可标记）
     */
    void handle(Long id, String remark);
}
