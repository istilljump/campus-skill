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
 * 管理端-技能者认证审核
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("技能者认证审核模型")
public class RunnerAuditProcessDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //审核记录id
    @ApiModelProperty(value = "审核记录id", required = true)
    @NotNull(message = "审核记录id不能为空")
    private Long id;

    //审核结果 1通过 2驳回
    @ApiModelProperty(value = "审核结果 1通过 2驳回", required = true)
    @NotNull(message = "审核结果不能为空")
    private Integer status;

    //审核备注
    @ApiModelProperty("审核备注")
    private String auditRemark;
}
