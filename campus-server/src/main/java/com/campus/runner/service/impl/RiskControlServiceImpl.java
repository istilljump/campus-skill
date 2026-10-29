package com.campus.runner.service.impl;

import com.campus.runner.entity.RiskControlLog;
import com.campus.runner.exception.BusinessException;
import com.campus.runner.mapper.RiskControlLogMapper;
import com.campus.runner.result.PageResult;
import com.campus.runner.service.RiskControlService;
import com.campus.runner.service.risk.RiskEvent;
import com.campus.runner.service.risk.RiskRule;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风控引擎实现：
 * - fire 同步执行于业务事务内：规则计数能读到本次未提交写入（如第 5 单发单必须数到自己），
 *   窗口口径才准确；单条规则异常只吞掉本条并 WARN，绝不影响业务主流程与其余规则；
 * - 命中统一落库 risk_control_log，等级 ≥ 3 额外 ERROR 告警。
 */
@Service
@Slf4j
public class RiskControlServiceImpl implements RiskControlService {

    @Autowired
    private List<RiskRule> rules;

    @Autowired
    private RiskControlLogMapper riskControlLogMapper;

    @Override
    public void fire(RiskEvent event) {
        if (event == null || event.getType() == null) {
            return;
        }
        for (RiskRule rule : rules) {
            try {
                if (!rule.supportedTriggers().contains(event.getType())) {
                    continue;
                }
                RiskRule.RiskFinding finding = rule.evaluate(event);
                if (finding == null) {
                    continue;
                }
                RiskControlLog logRecord = RiskControlLog.builder()
                        .ruleCode(rule.code())
                        .ruleName(rule.name())
                        .riskLevel(finding.getLevel())
                        .targetType(finding.getTargetType())
                        .targetId(finding.getTargetId())
                        .orderId(finding.getOrderId())
                        .detail(finding.getDetail())
                        .status(0)
                        .createTime(LocalDateTime.now())
                        .build();
                riskControlLogMapper.insert(logRecord);
                if (finding.getLevel() >= 3) {
                    log.error("[风控]{} {} 命中：{}", rule.code(), rule.name(), finding.getDetail());
                } else {
                    log.warn("[风控]{} {} 命中：{}", rule.code(), rule.name(), finding.getDetail());
                }
            } catch (Exception e) {
                //规则是外挂组件：单条规则故障绝不影响业务主流程与其余规则
                log.error("[风控]规则 {} 执行异常，已跳过：{}", rule.code(), e.getMessage(), e);
            }
        }
    }

    @Override
    public PageResult<RiskControlLog> page(Integer status, Integer level, Integer page, Integer pageSize) {
        PageHelper.startPage(page, pageSize);
        Page<RiskControlLog> logs = riskControlLogMapper.page(status, level);
        return new PageResult<>(logs.getTotal(), logs.getResult(), logs.getPageSize(), logs.getPageNum());
    }

    @Override
    public void handle(Long id, String remark) {
        RiskControlLog exists = riskControlLogMapper.getById(id);
        if (exists == null) {
            throw new BusinessException("风控告警不存在");
        }
        if (exists.getStatus() != null && exists.getStatus() == 1) {
            throw new BusinessException("该告警已处置，请勿重复处理");
        }
        int rows = riskControlLogMapper.markHandled(id, remark);
        if (rows == 0) {
            throw new BusinessException("处置失败，请刷新后重试");
        }
        log.info("[风控]告警处置完成，id={}, 规则={}，备注={}", id, exists.getRuleCode(), remark);
    }
}
