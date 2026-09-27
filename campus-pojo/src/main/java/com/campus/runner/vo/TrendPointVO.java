package com.campus.runner.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 数据大屏-每日趋势点
 */
@Data
@Builder
public class TrendPointVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 日期 yyyy-MM-dd */
    private String date;
    /** 当日新增订单数 */
    private Integer orderCount;
    /** 当日完成订单数（已送达/已完成） */
    private Integer completedCount;
    /** 当日交易额（已支付订单悬赏合计） */
    private BigDecimal amount;
}
