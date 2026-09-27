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
 * 提现记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("提现记录")
public class WithdrawRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //提现申请id
    private Long id;

    //提现金额
    private BigDecimal amount;

    //状态 0待处理 1已打款 2已驳回
    private Integer status;

    //申请时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;

    //打款时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    //备注
    private String remark;
}
