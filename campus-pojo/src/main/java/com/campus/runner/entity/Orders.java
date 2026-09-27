package com.campus.runner.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跑腿订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orders implements Serializable {

    /**
     * 订单状态 1待支付 2待接单 3进行中 4已送达 5已完成 6已取消 7已超时
     */
    public static final Integer PENDING_PAYMENT = 1;
    public static final Integer TO_BE_TAKEN = 2;
    public static final Integer IN_PROGRESS = 3;
    public static final Integer DELIVERED = 4;
    public static final Integer COMPLETED = 5;
    public static final Integer CANCELLED = 6;
    public static final Integer TIMEOUT = 7;

    /**
     * 支付状态 0未支付 1已支付 2已退款
     */
    public static final Integer UN_PAID = 0;
    public static final Integer PAID = 1;
    public static final Integer REFUND = 2;

    /**
     * 取消方 1用户 2跑腿员 3平台
     */
    public static final Integer CANCEL_BY_USER = 1;
    public static final Integer CANCEL_BY_RUNNER = 2;
    public static final Integer CANCEL_BY_PLATFORM = 3;

    private static final long serialVersionUID = 1L;

    private Long id;

    //订单号
    private String number;

    //发单用户id
    private Long userId;

    //跑腿员id（接单后回填）
    private Long runnerId;

    //订单类型id
    private Long typeId;

    //订单标题
    private String title;

    //需求描述
    private String description;

    //取件地址
    private String pickupAddress;

    //送达地址
    private String deliveryAddress;

    //校区
    private String campus;

    //悬赏金额
    private BigDecimal rewardAmount;

    //平台服务费
    private BigDecimal platformFee;

    //跑腿员实得金额
    private BigDecimal runnerIncome;

    //订单状态 1待支付 2待接单 3进行中 4已送达 5已完成 6已取消 7已超时
    private Integer status;

    //期望完成时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedTime;

    //超时时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timeoutTime;

    //取消原因
    private String cancelReason;

    //取消方 1用户 2跑腿员 3平台
    private Integer cancelBy;

    //是否申诉 0否 1是
    private Integer isAppealed;

    //支付方式 1微信 2钱包余额
    private Integer payMethod;

    //支付状态 0未支付 1已支付 2已退款
    private Integer payStatus;

    //下单时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderTime;

    //支付时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    //取件时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pickupTime;

    //完成时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    //取消时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
