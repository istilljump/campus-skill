package com.campus.runner.controller.user;

import com.campus.runner.dto.OrdersBoostDTO;
import com.campus.runner.dto.OrdersCancelDTO;
import com.campus.runner.dto.OrdersPageQueryDTO;
import com.campus.runner.dto.OrdersSubmitDTO;
import com.campus.runner.context.BaseContext;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.OrderService;
import com.campus.runner.vo.OrderDetailVO;
import com.campus.runner.vo.OrderSubmitVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 用户端-跑腿订单接口
 */
@RestController("userOrderController")
@Slf4j
@Api(tags = "用户端-跑腿订单接口")
@RequestMapping("/user/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/submit")
    @ApiOperation("发布订单")
    public Result<OrderSubmitVO> submit(@RequestBody @Valid OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户发布订单：{}", ordersSubmitDTO);
        return Result.success(orderService.submit(BaseContext.getCurrentId(), ordersSubmitDTO));
    }

    @PostMapping("/pay/{orderNumber}")
    @ApiOperation("订单支付")
    public Result<String> pay(@PathVariable String orderNumber) {
        orderService.pay(orderNumber, BaseContext.getCurrentId());
        return Result.success("支付成功");
    }

    @GetMapping("/page")
    @ApiOperation("我的订单分页查询")
    public Result<PageResult<OrderDetailVO>> page(OrdersPageQueryDTO ordersPageQueryDTO) {
        return Result.success(orderService.userPage(BaseContext.getCurrentId(), ordersPageQueryDTO));
    }

    @GetMapping("/detail/{id}")
    @ApiOperation("订单详情")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    @PutMapping("/confirm/{id}")
    @ApiOperation("确认完成")
    public Result<String> confirm(@PathVariable Long id) {
        orderService.confirm(id, BaseContext.getCurrentId());
        return Result.success("订单已完成");
    }

    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result<String> cancel(@RequestBody @Valid OrdersCancelDTO ordersCancelDTO) {
        //1-用户取消
        orderService.cancel(BaseContext.getCurrentId(), 1, ordersCancelDTO);
        return Result.success("订单已取消");
    }

    @PutMapping("/appeal/{id}")
    @ApiOperation("订单申诉")
    public Result<String> appeal(@PathVariable Long id) {
        orderService.appeal(id, BaseContext.getCurrentId());
        return Result.success("申诉已提交");
    }

    @PutMapping("/boost")
    @ApiOperation("订单追加悬赏（待接单且已支付）")
    public Result<String> boost(@RequestBody @Valid OrdersBoostDTO ordersBoostDTO) {
        orderService.boost(BaseContext.getCurrentId(), ordersBoostDTO);
        return Result.success("悬赏追加成功");
    }
}
