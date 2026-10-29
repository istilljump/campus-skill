package com.campus.runner.task;

import com.campus.runner.service.ReconciliationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 资金对账定时任务：每小时全量核对一次「钱包余额 = 流水重算值」恒等式，
 * 差异落 reconcile_record 表并 ERROR 告警，管理端可查（对账中心）。
 * 支付/提现等资金操作另有幂等令牌防护（IdempotentAspect），本任务是对账层面的最后防线。
 */
@Component
@Slf4j
public class ReconcileTask {

    @Autowired
    private ReconciliationService reconciliationService;

    //每小时整点执行一次全量对账
    @Scheduled(cron = "0 0 * * * ?")
    public void reconcile() {
        log.info("定时资金对账开始...");
        reconciliationService.reconcileAll();
    }
}
