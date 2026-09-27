package com.campus.runner.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 用户端-订单追加悬赏
 */
@Data
public class OrdersBoostDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "订单id不能为空")
    private Long id;

    @NotNull(message = "追加金额不能为空")
    @DecimalMin(value = "0.5", message = "追加金额至少0.5元")
    @DecimalMax(value = "100", message = "单次追加金额不能超过100元")
    private BigDecimal amount;
}
