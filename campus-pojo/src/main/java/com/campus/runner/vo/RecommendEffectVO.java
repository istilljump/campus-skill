package com.campus.runner.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 推荐效果统计（管理端）：按行为来源对比浏览量与预约/成交转化率
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("推荐效果统计")
public class RecommendEffectVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //行为来源 1市场列表 2猜你喜欢 3看了又看 4其他
    private Integer source;

    //来源名称
    private String sourceName;

    //浏览量
    private Long views;

    //转化数（预约 + 成交）
    private Long conversions;

    //转化率 = 转化数 / 浏览量
    private BigDecimal ctr;
}
