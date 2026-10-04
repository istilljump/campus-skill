package com.campus.runner.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 技能服务货架（技能者挂牌的可预约服务）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItem implements Serializable {

    /**
     * 状态 0下架 1上架 2封禁
     */
    public static final Integer OFF_SHELF = 0;
    public static final Integer ON_SHELF = 1;
    public static final Integer BANNED = 2;

    /**
     * 服务形式 1线上交付 2线下进行
     */
    public static final Integer MODE_ONLINE = 1;
    public static final Integer MODE_OFFLINE = 2;

    private static final long serialVersionUID = 1L;

    private Long id;

    //技能者id
    private Long skillerId;

    //技能类目id
    private Long categoryId;

    //服务标题
    private String title;

    //服务说明（含交付物形式与修改轮次）
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
}
