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
 * 管理端-纠纷仲裁判决
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("仲裁判决模型")
public class DisputeVerdictDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //仲裁结果 1退款用户 2放款技能者 3驳回
    @ApiModelProperty(value = "仲裁结果 1退款用户 2放款技能者 3驳回", required = true)
    @NotNull(message = "仲裁结果不能为空")
    private Integer status;

    //仲裁意见
    @ApiModelProperty("仲裁意见")
    private String verdict;
}
