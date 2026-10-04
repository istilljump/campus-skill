package com.campus.runner.controller.runner;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.OrdersCancelDTO;
import com.campus.runner.dto.OrdersDeliverDTO;
import com.campus.runner.dto.OrdersGrabDTO;
import com.campus.runner.dto.OrdersPageQueryDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.OrderService;
import com.campus.runner.service.RunnerService;
import com.campus.runner.vo.OrderDetailVO;
import com.campus.runner.vo.OrderHallVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 技能者端-订单接口（抢单、取件、送达、我的订单）
 */
@RestController
@Slf4j
@Api(tags = "技能者端-订单接口")
@RequestMapping("/skiller/orders")
public class RunnerOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RunnerService runnerService;

    @GetMapping("/hall")
    @ApiOperation("订单大厅分页查询")
    public Result<PageResult<OrderHallVO>> hall(OrdersPageQueryDTO ordersPageQueryDTO) {
        return Result.success(orderService.hallPage(ordersPageQueryDTO));
    }

    @PostMapping("/grab")
    @ApiOperation("抢单")
    public Result<String> grab(@RequestBody @Valid OrdersGrabDTO ordersGrabDTO) {
        Long runnerId = BaseContext.getCurrentId();
        orderService.grab(runnerId, ordersGrabDTO);
        return Result.success("抢单成功");
    }

    @PutMapping("/pickup/{id}")
    @ApiOperation("确认取件")
    public Result<String> pickup(@PathVariable Long id) {
        orderService.pickup(id, BaseContext.getCurrentId());
        return Result.success("取件成功");
    }

    @PutMapping("/deliver/{id}")
    @ApiOperation("提交交付物（已交付，等待用户验收，48小时未验收自动确认）")
    public Result<String> deliver(@PathVariable Long id, @RequestBody @Valid OrdersDeliverDTO ordersDeliverDTO) {
        orderService.deliver(id, BaseContext.getCurrentId(), ordersDeliverDTO);
        return Result.success("交付成功，等待用户验收");
    }

    @PostMapping("/rework/{id}")
    @ApiOperation("响应返修（返修中→进行中，重新交付）")
    public Result<String> respondRework(@PathVariable Long id) {
        orderService.respondRework(id, BaseContext.getCurrentId());
        return Result.success("已响应返修，请重新交付");
    }

    @GetMapping("/page")
    @ApiOperation("我的接单分页")
    public Result<PageResult<OrderDetailVO>> page(OrdersPageQueryDTO ordersPageQueryDTO,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer pageSize) {
        if (ordersPageQueryDTO.getPage() == null) {
            ordersPageQueryDTO.setPage(page == null ? 1 : page);
        }
        if (ordersPageQueryDTO.getPageSize() == null) {
            ordersPageQueryDTO.setPageSize(pageSize == null ? 10 : pageSize);
        }
        return Result.success(orderService.runnerPage(BaseContext.getCurrentId(), ordersPageQueryDTO));
    }

    @PutMapping("/cancel")
    @ApiOperation("取消订单（进行中订单作废并退款）")
    public Result<String> cancel(@RequestBody @Valid OrdersCancelDTO ordersCancelDTO) {
        //2-技能者取消
        orderService.cancel(BaseContext.getCurrentId(), 2, ordersCancelDTO);
        return Result.success("订单已取消");
    }
}
