package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 技能者端-提现申请
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("提现申请模型")
public class WithdrawApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //提现金额
    @ApiModelProperty(value = "提现金额", required = true)
    @NotNull(message = "提现金额不能为空")
    @DecimalMin(value = "0.01", message = "提现金额必须大于0")
    private BigDecimal amount;

    //提现密码
    @ApiModelProperty(value = "提现密码", required = true)
    @NotBlank(message = "提现密码不能为空")
    private String withdrawPassword;
}
