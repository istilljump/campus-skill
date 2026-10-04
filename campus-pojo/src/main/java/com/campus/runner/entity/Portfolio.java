package com.campus.runner.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 技能作品集
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio implements Serializable {

    /**
     * 审核状态 0待审核 1通过 2驳回
     */
    public static final Integer PENDING = 0;
    public static final Integer PASSED = 1;
    public static final Integer REJECTED = 2;

    private static final long serialVersionUID = 1L;

    private Long id;

    //技能者id
    private Long skillerId;

    //作品标题
    private String title;

    //所属技能类目id
    private Long categoryId;

    //封面图
    private String coverUrl;

    //作品文件URL列表，逗号分隔
    private String workUrls;

    //作品说明
    private String description;

    //审核状态 0待审核 1通过 2驳回
    private Integer status;

    //审核意见
    private String auditOpinion;

    //审核人id
    private Long auditorId;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
