package com.campus.runner.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内消息（用户端/技能者端共用收件箱，recipient_id 为用户ID）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long recipientId;
    private String title;
    private String content;
    private Long orderId;
    private Integer isRead;
    // 消息接口直接返回实体，需显式声明时间格式，否则会被序列化成数字数组
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
