package com.campus.runner.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金对账差异记录（仅记余额与流水重算值不一致的钱包，一轮对账一个批次号）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //对账批次号，如 RC20261029120000001
    private String batchNo;

    private Long walletId;

    private Long userId;

    //当前余额
    private BigDecimal balance;

    //按流水重算的期望余额
    private BigDecimal expectedBalance;

    //差异金额 = 余额 - 期望余额
    private BigDecimal diffAmount;

    //该钱包流水笔数
    private Integer txCount;

    //本轮对账钱包总数（批次冗余）
    private Integer checkedWallets;

    private LocalDateTime createTime;
}
