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
 * 技能者端-交付订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("订单交付模型")
public class OrdersDeliverDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //交付物地址（文件/图片URL）
    @ApiModelProperty(value = "交付物地址", required = true)
    @NotBlank(message = "请填写交付物地址")
    private String deliverableUrl;

    //交付说明
    @ApiModelProperty("交付说明")
    private String deliverableNote;
}
