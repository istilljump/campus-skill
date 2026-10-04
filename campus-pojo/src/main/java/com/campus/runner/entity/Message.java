package com.campus.runner.entity;

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
    private LocalDateTime createTime;
}
