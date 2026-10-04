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
 * 技能者信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("技能者信息")
public class RunnerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //技能者id
    private Long id;

    //关联用户id
    private Long userId;

    //技能者姓名
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

    //技能等级 1普通 2铜牌 3银牌 4金牌
    private Integer runnerLevel;

    //每日接单上限
    private Integer dailyOrderLimit;

    //完成订单数
    private Integer completedOrders;

    //综合评分
    private BigDecimal score;

    //账号状态 0禁用 1启用
    private Integer status;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
