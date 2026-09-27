package com.campus.runner.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 跑腿员统计（跑腿员端工作台）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("跑腿员统计")
public class RunnerStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //今日已接单数
    private Integer todayOrderCount;

    //今日收入
    private BigDecimal todayIncome;

    //本月完成订单数
    private Integer monthOrderCount;

    //本月收入
    private BigDecimal monthIncome;

    //完成订单总数
    private Integer totalCompletedOrders;

    //累计收入
    private BigDecimal totalIncome;

    //综合评分
    private BigDecimal score;
}
