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
 * 管理端-作品审核处理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("作品审核处理模型")
public class PortfolioAuditProcessDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //作品id
    @ApiModelProperty(value = "作品id", required = true)
    @NotNull(message = "作品id不能为空")
    private Long id;

    //审核结果 1通过 2驳回
    @ApiModelProperty(value = "审核结果 1通过 2驳回", required = true)
    @NotNull(message = "审核结果不能为空")
    private Integer status;

    //审核意见
    @ApiModelProperty("审核意见")
    private String auditOpinion;

    //作品定级 1 C1 2 C2 3 C3（通过时可选）
    @ApiModelProperty("作品定级 1 C1 2 C2 3 C3")
    private Integer skillLevel;
}
