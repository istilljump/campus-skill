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
 * 类目供需对比（管理端供需缺口看板）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("类目供需对比")
public class SupplyDemandVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("类目ID")
    private Long typeId;

    @ApiModelProperty("类目名称")
    private String typeName;

    @ApiModelProperty("供给：在售服务数")
    private Integer supplyCount;

    @ApiModelProperty("需求：近 30 天发单量")
    private Integer demandCount;

    @ApiModelProperty("历史平均成交价（已完成订单）")
    private BigDecimal avgPrice;
}
