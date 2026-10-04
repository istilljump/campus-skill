package com.campus.runner.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 服务预约单（服务预约模式）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking implements Serializable {

    /**
     * 状态 0待确认 1已确认 2已完成 3已拒绝 4已取消
     */
    public static final Integer PENDING_CONFIRM = 0;
    public static final Integer CONFIRMED = 1;
    public static final Integer COMPLETED = 2;
    public static final Integer REJECTED = 3;
    public static final Integer CANCELLED = 4;

    private static final long serialVersionUID = 1L;

    private Long id;

    //服务id
    private Long serviceItemId;

    //关联订单id（支付后回填）
    private Long orderId;

    //预约用户id
    private Long userId;

    //技能者id
    private Long skillerId;

    //期望开始时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectTime;

    //需求备注
    private String remark;

    //状态 0待确认 1已确认 2已完成 3已拒绝 4已取消
    private Integer status;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
