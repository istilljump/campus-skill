package com.campus.runner.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 跑腿员个人中心（含当日数据）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("跑腿员个人中心")
public class RunnerCenterVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //跑腿员id
    private Long id;

    //姓名
    private String name;

    //认证状态 0待认证 1已认证 2认证拒绝 3认证审核中
    private Integer auditStatus;

    //跑腿等级
    private Integer runnerLevel;

    //每日接单上限
    private Integer dailyOrderLimit;

    //今日已接单数
    private Integer todayOrderCount;

    //今日收入
    private BigDecimal todayIncome;

    //完成订单总数
    private Integer completedOrders;

    //综合评分
    private BigDecimal score;

    //钱包余额
    private BigDecimal balance;
}
