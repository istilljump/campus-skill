package com.campus.runner.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 数据大屏-交易转化漏斗阶段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("交易转化漏斗阶段")
public class FunnelStageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("阶段名：发布/支付/接单/交付/完成")
    private String name;

    @ApiModelProperty("该阶段订单数")
    private Integer value;
}
