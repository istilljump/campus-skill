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
 * 提现申请
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawRequest implements Serializable {

    /**
     * 状态 0待处理 1已打款 2已驳回
     */
    public static final Integer PENDING = 0;
    public static final Integer PAID = 1;
    public static final Integer REJECTED = 2;

    private static final long serialVersionUID = 1L;

    private Long id;

    //用户id
    private Long userId;

    //提现金额
    private BigDecimal amount;

    //状态 0待处理 1已打款 2已驳回
    private Integer status;

    //申请时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;

    //审核时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    //打款时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    //备注
    private String remark;
}
