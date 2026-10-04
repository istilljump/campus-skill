package com.campus.runner.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单详情（三端通用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("订单详情")
public class OrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //订单id
    private Long id;

    //订单号
    private String number;

    //订单状态
    private Integer status;

    //订单标题
    private String title;

    //需求描述
    private String description;

    //订单类型id
    private Long typeId;

    //订单类型名称
    private String typeName;

    //撮合模式 1悬赏 2服务预约
    private Integer mode;

    //关联服务货架id（预约模式）
    private Long serviceItemId;

    //取件地址
    private String pickupAddress;

    //送达地址
    private String deliveryAddress;

    //交付物地址（文件/图片URL）
    private String deliverableUrl;

    //交付说明
    private String deliverableNote;

    //校区
    private String campus;

    //悬赏金额
    private BigDecimal rewardAmount;

    //平台服务费
    private BigDecimal platformFee;

    //技能者实得金额
    private BigDecimal runnerIncome;

    //期望完成时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedTime;

    //超时时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timeoutTime;

    //取消原因
    private String cancelReason;

    //取消方 1用户 2技能者 3平台
    private Integer cancelBy;

    //是否申诉
    private Integer isAppealed;

    //支付状态
    private Integer payStatus;

    //发布者昵称（脱敏）
    private String publisherName;

    //发布者联系电话
    private String publisherPhone;

    //技能者姓名
    private String runnerName;

    //技能者联系电话
    private String runnerPhone;

    //技能者评分
    private BigDecimal runnerScore;

    //下单时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderTime;

    //支付时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    //交付时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliverTime;

    //返修次数
    private Integer reworkCount;

    //自动验收截止时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime autoAcceptTime;

    //完成时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
