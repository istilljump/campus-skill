package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 用户端-钱包充值
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("钱包充值模型")
public class WalletRechargeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //充值金额
    @ApiModelProperty(value = "充值金额", required = true)
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    private BigDecimal amount;
}
