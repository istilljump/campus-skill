package com.campus.runner.controller.admin;

import com.campus.runner.result.Result;
import com.campus.runner.service.StatisticsService;
import com.campus.runner.vo.OrderStatisticsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-数据统计看板
 */
@RestController
@Slf4j
@Api(tags = "管理端-数据统计接口")
@RequestMapping("/admin/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/overview")
    @ApiOperation("平台订单统计看板")
    public Result<OrderStatisticsVO> overview() {
        return Result.success(statisticsService.overview());
    }
}
