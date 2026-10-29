package com.campus.runner.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风控规则命中记录（规则引擎命中即落库，管理端可查可处置）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskControlLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //规则编码，如 R001
    private String ruleCode;

    //规则名称，如：高频发单
    private String ruleName;

    //风险等级 1低 2中 3高
    private Integer riskLevel;

    //对象类型 1用户 2技能者
    private Integer targetType;

    //对象id（target_type 对应 user.id / skiller.id）
    private Long targetId;

    private Long orderId;

    //命中详情（人类可读，含触发时的关键数值）
    private String detail;

    //处置状态 0未处置 1已处置
    private Integer status;

    private String handleRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
