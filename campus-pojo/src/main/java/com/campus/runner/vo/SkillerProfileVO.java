package com.campus.runner.vo;

import com.campus.runner.entity.Portfolio;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 技能者主页（用户端公开信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("技能者主页")
public class SkillerProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //技能者id
    private Long skillerId;

    //技能者姓名
    private String name;

    //校区
    private String campus;

    //学院
    private String college;

    //综合评分
    private BigDecimal score;

    //技能信用分
    private Integer creditScore;

    //作品定级 1 C1 2 C2 3 C3
    private Integer skillLevel;

    //完成订单数
    private Integer completedOrders;

    //作品集（仅已通过审核的）
    private List<Portfolio> portfolio;

    //上架的服务
    private List<ServiceItemVO> services;
}
