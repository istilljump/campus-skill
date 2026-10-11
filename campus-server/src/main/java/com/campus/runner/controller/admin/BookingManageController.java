package com.campus.runner.controller.admin;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-预约单管理
 */
@RestController
@Slf4j
@Api(tags = "管理端-预约单管理接口")
@RequestMapping("/admin/booking")
public class BookingManageController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/page")
    @ApiOperation("全平台预约单分页（可按状态过滤）")
    public Result<PageResult<BookingVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              Integer status) {
        return Result.success(bookingService.adminPage(status, page, pageSize));
    }

    @PutMapping("/{id}/cancel")
    @ApiOperation("管理员取消异常预约（仅待确认可取消，双端通知）")
    public Result<String> cancel(@PathVariable Long id) {
        bookingService.adminCancel(id);
        return Result.success("预约已取消");
    }
}
