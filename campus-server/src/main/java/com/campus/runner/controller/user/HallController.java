package com.campus.runner.controller.user;

import com.campus.runner.dto.OrdersPageQueryDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.OrderService;
import com.campus.runner.vo.OrderHallVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端/跑腿员端-订单大厅（待接单订单浏览）
 */
@RestController
@Slf4j
@Api(tags = "订单大厅接口")
@RequestMapping("/user/hall")
public class HallController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/list")
    @ApiOperation("订单大厅分页查询（跑腿员端登录后同样可访问）")
    public Result<PageResult<OrderHallVO>> list(OrdersPageQueryDTO ordersPageQueryDTO) {
        return Result.success(orderService.hallPage(ordersPageQueryDTO));
    }
}
