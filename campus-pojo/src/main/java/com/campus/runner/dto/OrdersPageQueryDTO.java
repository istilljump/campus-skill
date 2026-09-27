package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单分页查询（用户端我的订单 / 跑腿员端接单记录 / 管理端订单列表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("订单分页查询模型")
public class OrdersPageQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //页码
    private Integer page;

    //每页条数
    private Integer pageSize;

    //订单状态 1待支付 2待接单 3进行中 4已送达 5已完成 6已取消 7已超时
    private Integer status;

    //订单类型id
    private Long typeId;

    //校区
    private String campus;

    //订单号/标题模糊搜索
    private String keyword;
}
