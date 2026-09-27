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
 * 用户端/跑腿员端-订单取消
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("订单取消模型")
public class OrdersCancelDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //订单id
    @ApiModelProperty(value = "订单id", required = true)
    @NotNull(message = "订单id不能为空")
    private Long id;

    //取消原因
    @ApiModelProperty("取消原因")
    private String cancelReason;
}
