package com.campus.runner.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 类目历史成交价参考（发单页智能定价）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("类目价格参考")
public class PriceReferenceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("样本量（已完成订单数）")
    private Integer sampleCount;

    @ApiModelProperty("P25 分位成交价（低于 25% 的历史成交）")
    private BigDecimal p25;

    @ApiModelProperty("P50 中位成交价")
    private BigDecimal p50;

    @ApiModelProperty("P75 分位成交价（高于 75% 的历史成交）")
    private BigDecimal p75;

    @ApiModelProperty("平均交付时长（小时，下单到验收）")
    private Double avgDeliveryHours;
}
