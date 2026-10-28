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
 * 信用分流水（技能者端「信用档案」）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("信用分流水")
public class CreditLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //分值变动（正加负减）
    private Integer delta;

    //变动原因，如：按时交付+1/返修-3/仲裁判责-10
    private String reason;

    //关联订单id
    private Long orderId;

    //关联订单号
    private String orderNumber;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
