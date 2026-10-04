package com.campus.runner.controller.runner;

import com.campus.runner.context.BaseContext;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.BookingService;
import com.campus.runner.vo.BookingVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 技能者端-服务预约接口（确认/拒绝预约）
 */
@RestController
@Slf4j
@Api(tags = "技能者端-服务预约接口")
@RequestMapping("/skiller/booking")
public class SkillerBookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/page")
    @ApiOperation("收到的预约分页（可按状态过滤）")
    public Result<PageResult<BookingVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              Integer status) {
        return Result.success(bookingService.skillerPage(BaseContext.getCurrentId(), status, page, pageSize));
    }

    @PostMapping("/{id}/confirm")
    @ApiOperation("确认预约（待确认→已确认）")
    public Result<String> confirm(@PathVariable Long id) {
        bookingService.confirm(BaseContext.getCurrentId(), id);
        return Result.success("预约已确认");
    }

    @PostMapping("/{id}/reject")
    @ApiOperation("拒绝预约（待确认→已拒绝）")
    public Result<String> reject(@PathVariable Long id) {
        bookingService.reject(BaseContext.getCurrentId(), id);
        return Result.success("预约已拒绝");
    }
}
