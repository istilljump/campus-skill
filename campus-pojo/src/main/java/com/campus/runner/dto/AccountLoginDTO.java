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
 * 账号密码登录（用户端/技能者端共用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("账号密码登录模型")
public class AccountLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "登录账号", required = true)
    @NotBlank(message = "请输入登录账号")
    private String username;

    @ApiModelProperty(value = "登录密码", required = true)
    @NotBlank(message = "请输入登录密码")
    private String password;
}
