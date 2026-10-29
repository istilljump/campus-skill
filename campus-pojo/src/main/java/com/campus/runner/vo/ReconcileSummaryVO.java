package com.campus.runner.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对账批次汇总（不含差异明细）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //批次号
    private String batchNo;

    //本轮检查的钱包总数
    private Long checkedWallets;

    //差异数
    private Long mismatchCount;

    //一致数
    private Long cleanCount;

    //是否全部一致（差异数为 0）
    private Boolean allMatched;

    private LocalDateTime createTime;
}
