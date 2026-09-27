package com.campus.runner.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单评价
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReview implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //订单id
    private Long orderId;

    //发单用户id
    private Long userId;

    //跑腿员id
    private Long runnerId;

    //评分 1-5
    private Integer score;

    //评价内容
    private String content;

    //评价标签，逗号分隔
    private String tags;

    //是否匿名 0否 1是
    private Integer isAnonymous;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
