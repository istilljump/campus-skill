package com.campus.runner.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 技能订单类型（用户端展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("技能订单类型")
public class ErrandTypeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //类型id
    private Long id;

    //类型名称
    private String name;

    //图标
    private String icon;

    //类型说明
    private String description;

    //平台服务费率
    private BigDecimal feeRate;

    //排序权重
    private Integer sort;

    //状态 1启用 0停用
    private Integer status;
}
