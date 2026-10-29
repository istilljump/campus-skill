package com.campus.runner.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 数据大屏-资金与信任总览（v11 可信交易保障体系的看板视角）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("资金与信任总览")
public class TrustOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("托管中金额（已支付未结算的钱包支付订单悬赏合计）")
    private BigDecimal escrowAmount;

    @ApiModelProperty("钱包总余额（全部钱包账户可用余额合计）")
    private BigDecimal walletBalance;

    @ApiModelProperty("提现在途金额（待审核提现申请合计）")
    private BigDecimal withdrawPending;

    @ApiModelProperty("未处置风控告警数")
    private Integer riskUnhandled;

    @ApiModelProperty("近7天风控告警数")
    private Integer risk7d;

    @ApiModelProperty("最近对账批次号（null=尚未执行过对账）")
    private String reconcileBatchNo;

    @ApiModelProperty("最近批次差异钱包数")
    private Long reconcileMismatch;

    @ApiModelProperty("最近批次检查钱包数")
    private Long reconcileWallets;

    @ApiModelProperty("账实是否相符（最近批次差异为0）")
    private Boolean reconcileAllMatched;
}
