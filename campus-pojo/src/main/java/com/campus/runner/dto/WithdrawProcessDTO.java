package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 管理端-提现申请处理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("提现申请处理模型")
public class WithdrawProcessDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //提现申请id
    @ApiModelProperty(value = "提现申请id", required = true)
    @NotNull(message = "提现申请id不能为空")
    private Long id;

    //处理结果 1已打款 2已驳回
    @ApiModelProperty(value = "处理结果 1已打款 2已驳回", required = true)
    @NotNull(message = "处理结果不能为空")
    private Integer status;

    //备注
    @ApiModelProperty("备注")
    private String remark;
}
