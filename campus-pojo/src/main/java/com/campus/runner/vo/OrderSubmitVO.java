package com.campus.runner.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 发单提交结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("发单提交结果")
public class OrderSubmitVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //订单id
    private Long id;

    //订单号
    private String orderNumber;

    //悬赏金额
    private BigDecimal rewardAmount;

    //平台服务费
    private BigDecimal platformFee;

    //应付总金额（悬赏金额 + 平台服务费）
    private BigDecimal payAmount;
}
