package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 技能者端-服务货架发布/编辑
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("服务货架模型")
public class ServiceItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //技能类目id
    @ApiModelProperty(value = "技能类目id", required = true)
    @NotNull(message = "技能类目不能为空")
    private Long categoryId;

    //服务标题
    @ApiModelProperty(value = "服务标题", required = true)
    @NotBlank(message = "服务标题不能为空")
    private String title;

    //服务说明（含交付物形式与修改轮次）
    @ApiModelProperty("服务说明")
    private String description;

    //挂牌价
    @ApiModelProperty(value = "挂牌价", required = true)
    @NotNull(message = "挂牌价不能为空")
    @DecimalMin(value = "0.01", message = "挂牌价必须大于0")
    private BigDecimal price;

    //承诺交付天数
    @ApiModelProperty("承诺交付天数，默认3")
    private Integer deliveryDays;

    //服务形式 1线上交付 2线下进行
    @ApiModelProperty("服务形式 1线上交付 2线下进行，默认1")
    private Integer serviceMode;

    //技能标签，逗号分隔
    @ApiModelProperty("技能标签，逗号分隔")
    private String tags;
}
