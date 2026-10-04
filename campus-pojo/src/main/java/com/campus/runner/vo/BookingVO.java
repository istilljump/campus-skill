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
 * 服务预约单展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("服务预约单展示")
public class BookingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //预约单id
    private Long id;

    //服务id
    private Long serviceItemId;

    //关联订单id（支付后回填）
    private Long orderId;

    //预约用户id
    private Long userId;

    //技能者id
    private Long skillerId;

    //期望开始时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectTime;

    //需求备注
    private String remark;

    //状态 0待确认 1已确认 2已完成 3已拒绝 4已取消
    private Integer status;

    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    //服务标题
    private String serviceTitle;

    //服务价格
    private BigDecimal servicePrice;

    //预约用户姓名
    private String userName;

    //技能者姓名
    private String skillerName;
}
