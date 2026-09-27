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
 * 钱包流水
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction implements Serializable {

    /**
     * 流水类型 1跑腿收入 2支付支出 3充值 4提现 5退款 6违约金
     */
    public static final Integer INCOME = 1;
    public static final Integer EXPENSE = 2;
    public static final Integer RECHARGE = 3;
    public static final Integer WITHDRAW = 4;
    public static final Integer REFUND = 5;
    public static final Integer PENALTY = 6;

    private static final long serialVersionUID = 1L;

    private Long id;

    //钱包账户id
    private Long walletId;

    //流水类型 1跑腿收入 2支付支出 3充值 4提现 5退款 6违约金
    private Integer type;

    //发生金额
    private BigDecimal amount;

    //关联订单id
    private Long orderId;

    //备注
    private String remark;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
