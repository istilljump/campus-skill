package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 修改登录密码
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("修改登录密码模型")
public class PasswordChangeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "原密码", required = true)
    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    @ApiModelProperty(value = "新密码（6-32位）", required = true)
    @NotBlank(message = "请输入新密码")
    @Pattern(regexp = "^\\S{6,32}$", message = "新密码为6-32位，不能含空格")
    private String newPassword;
}
