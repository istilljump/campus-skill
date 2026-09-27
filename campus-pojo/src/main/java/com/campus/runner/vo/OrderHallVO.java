package com.campus.runner.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单大厅列表项（跑腿员抢单视图）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("订单大厅列表项")
public class OrderHallVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //订单id
    private Long id;

    //订单号
    private String number;

    //订单标题
    private String title;

    //需求描述
    private String description;

    //订单类型id
    private Long typeId;

    //订单类型名称
    private String typeName;

    //取件地址
    private String pickupAddress;

    //送达地址
    private String deliveryAddress;

    //校区
    private String campus;

    //悬赏金额
    private BigDecimal rewardAmount;

    //跑腿员实得金额
    private BigDecimal runnerIncome;

    //期望完成时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedTime;

    //发布者昵称（脱敏）
    private String publisherName;

    //发布时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
