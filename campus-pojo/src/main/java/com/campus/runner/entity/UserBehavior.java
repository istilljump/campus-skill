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
 * 用户行为记录（推荐引擎数据源）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehavior implements Serializable {

    /**
     * 行为类型 1浏览 2预约 3成交
     */
    public static final Integer TYPE_VIEW = 1;
    public static final Integer TYPE_BOOKING = 2;
    public static final Integer TYPE_DEAL = 3;

    /**
     * 行为来源 1市场列表 2猜你喜欢 3看了又看 4其他（推荐效果实验用）
     */
    public static final Integer SOURCE_MARKET = 1;
    public static final Integer SOURCE_RECOMMEND = 2;
    public static final Integer SOURCE_SIMILAR = 3;
    public static final Integer SOURCE_OTHER = 4;

    private static final long serialVersionUID = 1L;

    private Long id;

    //用户id
    private Long userId;

    //技能服务id
    private Long serviceItemId;

    //技能类目id（行为发生时冗余）
    private Long categoryId;

    //行为类型 1浏览 2预约 3成交
    private Integer behaviorType;

    //行为来源 1市场列表 2猜你喜欢 3看了又看 4其他
    private Integer source;

    //行为权重（浏览1.0/预约3.0/成交5.0）
    private BigDecimal weight;

    //行为时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
