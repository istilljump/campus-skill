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
 * 技能订单类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrandType implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //类型名称（如：帮我取、帮我送、代买）
    private String name;

    //图标
    private String icon;

    //类型说明
    private String description;

    //平台服务费率
    private BigDecimal feeRate;

    //排序
    private Integer sort;

    //状态 0停用 1启用
    private Integer status;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
