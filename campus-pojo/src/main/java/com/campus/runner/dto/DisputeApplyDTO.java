package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户端-纠纷仲裁申请
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("纠纷仲裁申请模型")
public class DisputeApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //纠纷类型 1质量不符 2延期 3其他
    @ApiModelProperty(value = "纠纷类型 1质量不符 2延期 3其他", required = true)
    @NotNull(message = "纠纷类型不能为空")
    private Integer reasonType;

    //纠纷描述
    @ApiModelProperty("纠纷描述")
    @Size(max = 500, message = "纠纷描述不能超过500字")
    private String description;

    //凭证URL列表，逗号分隔
    @ApiModelProperty("凭证URL列表，逗号分隔")
    private String evidenceUrls;
}
