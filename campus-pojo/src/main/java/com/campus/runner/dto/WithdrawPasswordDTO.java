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
 * 技能者端-设置/修改提现密码
 * <p>密码通过请求体传输，避免出现在 URL 查询串与访问日志中。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("提现密码设置模型")
public class WithdrawPasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //提现密码（6 位数字）
    @ApiModelProperty(value = "提现密码（6 位数字）", required = true)
    @NotBlank(message = "提现密码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "提现密码必须为 6 位数字")
    private String password;
}
