package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 管理端-跑腿订单类型维护
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("跑腿订单类型模型")
public class ErrandTypeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //类型id（修改时必填）
    @ApiModelProperty("类型id")
    private Long id;

    //类型名称
    @ApiModelProperty(value = "类型名称", required = true)
    @NotBlank(message = "类型名称不能为空")
    private String name;

    //图标
    @ApiModelProperty("图标")
    private String icon;

    //类型说明
    @ApiModelProperty("类型说明")
    private String description;

    //平台服务费率 0-1
    @ApiModelProperty(value = "平台服务费率", required = true)
    @DecimalMin(value = "0", message = "服务费率不能小于0")
    @DecimalMax(value = "1", message = "服务费率不能大于1")
    private BigDecimal feeRate;

    //排序
    @ApiModelProperty("排序")
    private Integer sort;

    //状态 0停用 1启用
    @ApiModelProperty("状态 0停用 1启用")
    private Integer status;
}
