package com.campus.runner.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户端-发单提交
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("发单提交模型")
public class OrdersSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //订单类型id
    @ApiModelProperty(value = "订单类型id", required = true)
    @NotNull(message = "订单类型不能为空")
    private Long typeId;

    //订单标题
    @ApiModelProperty(value = "订单标题", required = true)
    @NotBlank(message = "订单标题不能为空")
    private String title;

    //需求描述
    @ApiModelProperty("需求描述")
    private String description;

    //取件地址
    @ApiModelProperty(value = "取件地址", required = true)
    @NotBlank(message = "取件地址不能为空")
    private String pickupAddress;

    //送达地址id（关联地址簿）
    @ApiModelProperty(value = "送达地址id", required = true)
    @NotNull(message = "送达地址不能为空")
    private Long addressBookId;

    //送达地址快照（不依赖地址簿时直接填入）
    @ApiModelProperty("送达地址快照")
    private String deliveryAddress;

    //校区
    @ApiModelProperty("校区")
    private String campus;

    //悬赏金额
    @ApiModelProperty(value = "悬赏金额", required = true)
    @NotNull(message = "悬赏金额不能为空")
    @DecimalMin(value = "0.01", message = "悬赏金额必须大于0")
    private BigDecimal rewardAmount;

    //期望完成时间
    @ApiModelProperty("期望完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedTime;

    //支付方式 1微信 2钱包余额
    @ApiModelProperty("支付方式 1微信 2钱包余额")
    private Integer payMethod;
}
