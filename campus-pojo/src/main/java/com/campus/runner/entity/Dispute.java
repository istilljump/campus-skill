package com.campus.runner.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 纠纷仲裁工单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispute implements Serializable {

    /**
     * 状态 0待仲裁 1仲裁-退款用户 2仲裁-放款技能者 3已驳回
     */
    public static final Integer PENDING = 0;
    public static final Integer REFUND_USER = 1;
    public static final Integer RELEASE_SKILLER = 2;
    public static final Integer REJECTED = 3;

    private static final long serialVersionUID = 1L;

    private Long id;

    //订单id
    private Long orderId;

    //发起方用户id
    private Long raisedBy;

    //纠纷类型 1质量不符 2延期 3其他
    private Integer reasonType;

    //纠纷描述
    private String description;

    //凭证URL列表，逗号分隔
    private String evidenceUrls;

    //状态 0待仲裁 1仲裁-退款用户 2仲裁-放款技能者 3已驳回
    private Integer status;

    //仲裁意见
    private String verdict;

    //仲裁管理员id
    private Long adminId;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
