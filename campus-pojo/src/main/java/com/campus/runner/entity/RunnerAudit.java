package com.campus.runner.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 跑腿员认证审核
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunnerAudit implements Serializable {

    /**
     * 审核状态 0待审核 1通过 2驳回
     */
    public static final Integer PENDING = 0;
    public static final Integer PASSED = 1;
    public static final Integer REJECTED = 2;

    private static final long serialVersionUID = 1L;

    private Long id;

    //跑腿员id
    private Long runnerId;

    //真实姓名
    private String realName;

    //学号
    private String studentNo;

    //校区
    private String campus;

    //学院
    private String college;

    //身份证号
    private String idCard;

    //学生证照片
    private String studentCardImg;

    //审核状态 0待审核 1通过 2驳回
    private Integer status;

    //审核备注
    private String auditRemark;

    //审核人id
    private Long auditorId;

    //申请时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;

    //审核时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
}
