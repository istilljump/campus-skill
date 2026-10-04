package com.campus.runner.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务货架展示（用户端市场）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("服务货架展示")
public class ServiceItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //服务id
    private Long id;

    //技能者id
    private Long skillerId;

    //技能类目id
    private Long categoryId;

    //服务标题
    private String title;

    //服务说明
    private String description;

    //挂牌价
    private BigDecimal price;

    //承诺交付天数
    private Integer deliveryDays;

    //服务形式 1线上交付 2线下进行
    private Integer serviceMode;

    //技能标签，逗号分隔
    private String tags;

    //状态 0下架 1上架 2封禁
    private Integer status;

    //累计成交数
    private Integer salesCount;

    //服务均分
    private BigDecimal avgScore;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    //技能者姓名
    private String skillerName;

    //技能者综合评分
    private BigDecimal skillerScore;

    //技能者信用分
    private Integer skillerCreditScore;

    //技能者作品定级 1 C1 2 C2 3 C3
    private Integer skillerSkillLevel;

    //技能类目名称
    private String categoryName;
}
