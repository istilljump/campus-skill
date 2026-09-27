package com.campus.runner.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 平台订单统计（管理端工作台）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("平台订单统计")
public class OrderStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //订单总数
    private Integer totalOrders;

    //今日新增订单数
    private Integer todayOrders;

    //待接单数
    private Integer pendingCount;

    //进行中订单数
    private Integer inProgressCount;

    //已完成订单数
    private Integer completedCount;

    //已取消订单数
    private Integer cancelledCount;

    //流水总金额（悬赏金额合计）
    private BigDecimal totalReward;

    //平台服务费总收益
    private BigDecimal totalPlatformFee;
}
