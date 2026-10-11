package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 账号注册（用户端；认证通过后技能者端使用同一账号登录）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("账号注册模型")
public class AccountRegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "登录账号（4-20位字母/数字/下划线）", required = true)
    @NotBlank(message = "请输入登录账号")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "账号为4-20位字母、数字或下划线")
    private String username;

    @ApiModelProperty(value = "登录密码（6-32位）", required = true)
    @NotBlank(message = "请输入登录密码")
    @Pattern(regexp = "^\\S{6,32}$", message = "密码为6-32位，不能含空格")
    private String password;

    @ApiModelProperty(value = "昵称（选填，默认与账号相同）")
    @Size(max = 32, message = "昵称不能超过32字")
    private String name;
}
