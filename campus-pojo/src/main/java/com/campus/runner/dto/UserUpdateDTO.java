package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户端-个人资料编辑
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("个人资料编辑模型")
public class UserUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //昵称
    @ApiModelProperty("昵称")
    @Size(max = 32, message = "昵称最长32个字符")
    private String name;

    //手机号
    @ApiModelProperty("手机号")
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    //学号
    @ApiModelProperty("学号")
    @Size(max = 20, message = "学号最长20个字符")
    private String studentNo;

    //校区
    @ApiModelProperty("校区")
    @Size(max = 50, message = "校区最长50个字符")
    private String campus;
}
