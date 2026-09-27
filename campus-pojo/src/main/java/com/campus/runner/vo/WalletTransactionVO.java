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
 * 钱包流水明细
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("钱包流水明细")
public class WalletTransactionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //流水id
    private Long id;

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
