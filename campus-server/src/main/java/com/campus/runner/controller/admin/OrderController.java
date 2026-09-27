package com.campus.runner.controller.admin;

import com.campus.runner.dto.OrdersPageQueryDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.OrderService;
import com.campus.runner.vo.OrderDetailVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-订单管理
 */
@RestController("adminOrderController")
@Slf4j
@Api(tags = "管理端-订单管理接口")
@RequestMapping("/admin/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/page")
    @ApiOperation("订单列表分页")
    public Result<PageResult<OrderDetailVO>> page(OrdersPageQueryDTO ordersPageQueryDTO) {
        return Result.success(orderService.adminPage(ordersPageQueryDTO));
    }

    @GetMapping("/detail/{id}")
    @ApiOperation("订单详情")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }
}
