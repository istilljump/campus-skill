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
 * 跑腿员端-抢单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("跑腿员抢单模型")
public class OrdersGrabDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //订单id
    @ApiModelProperty(value = "订单id", required = true)
    @NotNull(message = "订单id不能为空")
    private Long id;
}
