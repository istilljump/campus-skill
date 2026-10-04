package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 技能者端-认证申请
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("技能者认证申请模型")
public class RunnerAuditApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //真实姓名
    @ApiModelProperty(value = "真实姓名", required = true)
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    //学号
    @ApiModelProperty(value = "学号", required = true)
    @NotBlank(message = "学号不能为空")
    private String studentNo;

    //校区
    @ApiModelProperty("校区")
    private String campus;

    //学院
    @ApiModelProperty("学院")
    private String college;

    //身份证号
    @ApiModelProperty("身份证号")
    private String idCard;

    //学生证照片
    @ApiModelProperty("学生证照片")
    private String studentCardImg;
}
