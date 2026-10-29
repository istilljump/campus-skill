package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 管理端-风控告警处置
 */
@Data
@ApiModel("风控告警处置模型")
public class RiskHandleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "告警id", required = true)
    @NotNull(message = "告警id不能为空")
    private Long id;

    @ApiModelProperty(value = "处置备注")
    private String remark;
}
