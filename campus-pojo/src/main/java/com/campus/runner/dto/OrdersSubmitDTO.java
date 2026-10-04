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
import javax.validation.constraints.Size;
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
    @Size(max = 100, message = "订单标题不能超过100字")
    private String title;

    //需求描述
    @ApiModelProperty("需求描述")
    @Size(max = 500, message = "需求描述不能超过500字")
    private String description;

    //取件/服务地点（线下服务或需要现场取材时填写，纯线上交付可不填）
    @ApiModelProperty("取件/服务地点（线下服务填写，线上交付可不填）")
    @Size(max = 255, message = "服务地点不能超过255字")
    private String pickupAddress;

    //送达地址id（关联地址簿），与送达地址快照二选一
    @ApiModelProperty("送达地址id（与送达地址快照二选一）")
    private Long addressBookId;

    //送达地址快照（不依赖地址簿时直接填入）
    @ApiModelProperty("送达地址快照")
    @Size(max = 255, message = "交付地址不能超过255字")
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
