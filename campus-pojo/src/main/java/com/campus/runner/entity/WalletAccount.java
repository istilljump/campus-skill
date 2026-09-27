package com.campus.runner.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包账户
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //用户id
    private Long userId;

    //可用余额
    private BigDecimal balance;

    //冻结金额
    private BigDecimal frozenAmount;

    //累计收入
    private BigDecimal totalIncome;

    //累计支出
    private BigDecimal totalExpense;

    //状态 0冻结 1正常
    private Integer status;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
