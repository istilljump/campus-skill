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
 * 技能者名片上的服务摘要（脱敏，仅标题/价格/销量）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("技能者名片服务摘要")
public class ServiceBriefVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("服务标题")
    private String title;

    @ApiModelProperty("价格")
    private BigDecimal price;

    @ApiModelProperty("销量")
    private Integer salesCount;
}
