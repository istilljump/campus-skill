package com.campus.runner.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 技能者信用分流水
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //技能者id
    private Long skillerId;

    //分值变动（正加负减）
    private Integer delta;

    //变动原因
    private String reason;

    //关联订单id
    private Long orderId;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
