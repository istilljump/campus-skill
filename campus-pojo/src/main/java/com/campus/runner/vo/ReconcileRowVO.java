package com.campus.runner.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 对账扫描行：钱包当前余额 + 按流水聚合的期望余额（ReconcileMapper.scanAll 结果）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long walletId;

    private Long userId;

    private BigDecimal balance;

    private BigDecimal expected;

    private Long txCount;
}
