package com.campus.runner.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
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
@ApiModel("钱包账户")
public class WalletVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //钱包账户id
    private Long id;

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

    //更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
