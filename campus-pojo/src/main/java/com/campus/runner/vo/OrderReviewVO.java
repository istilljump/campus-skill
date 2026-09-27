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
 * 订单评价
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("订单评价")
public class OrderReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //评价id
    private Long id;

    //订单id
    private Long orderId;

    //评分 1-5
    private Integer score;

    //评价内容
    private String content;

    //评价标签，逗号分隔
    private String tags;

    //是否匿名 0否 1是
    private Integer isAnonymous;

    //评价人昵称（匿名时脱敏）
    private String userName;

    //跑腿员姓名
    private String runnerName;

    //评价时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
