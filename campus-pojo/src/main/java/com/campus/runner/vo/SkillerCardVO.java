package com.campus.runner.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 技能者分享名片（公开脱敏：不含手机号/学号/学院等隐私字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("技能者分享名片")
public class SkillerCardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("技能者ID")
    private Long runnerId;

    @ApiModelProperty("展示名")
    private String name;

    @ApiModelProperty("等级（1-3）")
    private Integer runnerLevel;

    @ApiModelProperty("评分")
    private BigDecimal score;

    @ApiModelProperty("信用分")
    private Integer creditScore;

    @ApiModelProperty("累计完成订单数")
    private Integer completedOrders;

    @ApiModelProperty("在售服务 Top3（按销量）")
    private List<ServiceBriefVO> services;
}
