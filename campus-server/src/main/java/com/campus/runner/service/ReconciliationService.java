package com.campus.runner.service;

import com.campus.runner.vo.ReconcileSummaryVO;

public interface ReconciliationService {

    /**
     * 执行一轮全量资金对账：
     * 逐钱包比较「当前余额」与「按流水重算的期望余额」，差异落 reconcile_record 并告警。
     *
     * @return 批次汇总（检查数 / 差异数 / 一致数）
     */
    ReconcileSummaryVO reconcileAll();
}
