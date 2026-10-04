package com.campus.runner.controller.user;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.BookingApplyDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.BookingService;
import com.campus.runner.vo.BookingVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 用户端-服务预约接口（预约/取消/支付）
 */
@RestController
@Slf4j
@Api(tags = "用户端-服务预约接口")
@RequestMapping("/user/booking")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    @ApiOperation("预约服务（生成待确认预约单）")
    public Result<String> apply(@RequestBody @Valid BookingApplyDTO bookingApplyDTO) {
        bookingService.apply(BaseContext.getCurrentId(), bookingApplyDTO);
        return Result.success("预约申请已提交，等待技能者确认");
    }

    @GetMapping("/page")
    @ApiOperation("我的预约分页")
    public Result<PageResult<BookingVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(bookingService.userPage(BaseContext.getCurrentId(), page, pageSize));
    }

    @PutMapping("/{id}/cancel")
    @ApiOperation("取消预约（仅待确认可取消）")
    public Result<String> cancel(@PathVariable Long id) {
        bookingService.cancel(BaseContext.getCurrentId(), id);
        return Result.success("预约已取消");
    }

    @PostMapping("/{id}/pay")
    @ApiOperation("预约支付（技能者确认后下单，生成待支付订单）")
    public Result<OrderSubmitVO> pay(@PathVariable Long id) {
        return Result.success(bookingService.pay(BaseContext.getCurrentId(), id));
    }
}
