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
 * 跑腿员（完成认证的用户）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Runner implements Serializable {

    /**
     * 认证状态 0待认证 1已认证 2认证拒绝 3认证审核中
     */
    public static final Integer AUDIT_PENDING = 0;
    public static final Integer AUDIT_PASSED = 1;
    public static final Integer AUDIT_REJECTED = 2;
    public static final Integer AUDIT_REVIEWING = 3;

    private static final long serialVersionUID = 1L;

    private Long id;

    //关联用户id
    private Long userId;

    //跑腿员姓名
    private String name;

    //手机号
    private String phone;

    //学号
    private String studentNo;

    //校区
    private String campus;

    //学院
    private String college;

    //认证状态 0待认证 1已认证 2认证拒绝 3认证审核中
    private Integer auditStatus;

    //跑腿等级 1普通 2铜牌 3银牌 4金牌
    private Integer runnerLevel;

    //每日接单上限
    private Integer dailyOrderLimit;

    //完成订单数
    private Integer completedOrders;

    //综合评分
    private BigDecimal score;

    //提现密码(加密存储)
    private String withdrawPassword;

    //账号状态 0禁用 1启用
    private Integer status;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
