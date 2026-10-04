package com.campus.runner.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 技能者信用分排行
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("技能者信用分排行")
public class CreditRankVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //技能者id
    private Long skillerId;

    //技能者姓名
    private String name;

    //技能信用分
    private Integer creditScore;

    //作品定级 1 C1 2 C2 3 C3
    private Integer skillLevel;

    //综合评分
    private BigDecimal score;

    //完成订单数
    private Integer completedOrders;
}
