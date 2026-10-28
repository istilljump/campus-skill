package com.campus.runner.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 我发出的评价（用户端「我的评价」列表项）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("我的评价")
public class MyReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //订单id（可跳转订单详情）
    private Long orderId;

    //订单标题
    private String orderTitle;

    //技能者姓名（匿名评价时脱敏由前端处理）
    private String skillerName;

    //评分 1-5
    private Integer score;

    //评价内容
    private String content;

    //评价标签，逗号分隔
    private String tags;

    //是否匿名 0否 1是
    private Integer isAnonymous;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
