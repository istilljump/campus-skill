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
 * 技能者认证审核记录（管理端列表项）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("技能者认证审核记录")
public class RunnerAuditVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //审核记录id
    private Long id;

    //技能者id
    private Long runnerId;

    //关联用户id
    private Long userId;

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

    //申请时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;

    //审核时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
}
