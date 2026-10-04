package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户端-返修申请
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("返修申请模型")
public class ReworkDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //返修原因
    @ApiModelProperty("返修原因")
    @Size(max = 255, message = "返修原因不能超过255字")
    private String reason;
}
