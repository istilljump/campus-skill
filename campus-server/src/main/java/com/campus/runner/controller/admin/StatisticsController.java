package com.campus.runner.controller.admin;

import com.campus.runner.result.Result;
import com.campus.runner.service.RecommendService;
import com.campus.runner.service.StatisticsService;
import com.campus.runner.vo.NameCountVO;
import com.campus.runner.vo.OrderStatisticsVO;
import com.campus.runner.vo.SupplyDemandVO;
import com.campus.runner.vo.TrendPointVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Autowired
    private RecommendService recommendService;

    @GetMapping("/overview")
    @ApiOperation("平台订单统计看板")
    public Result<OrderStatisticsVO> overview() {
        return Result.success(statisticsService.overview());
    }

    @GetMapping("/trend")
    @ApiOperation("近 N 日订单趋势（默认7天）")
    public Result<List<TrendPointVO>> trend(@RequestParam(defaultValue = "7") Integer days) {
        return Result.success(statisticsService.dailyTrend(days));
    }

    @GetMapping("/recommendEffect")
    @ApiOperation("推荐效果对比（v8 实验）：近 N 天各行为来源的浏览量与预约/成交转化率")
    public Result<List<com.campus.runner.vo.RecommendEffectVO>> recommendEffect(@RequestParam(defaultValue = "7") Integer days) {
        return Result.success(recommendService.effectStats(days));
    }

    @GetMapping("/typeRank")
    @ApiOperation("技能类目订单量 TOP N（默认5）")
    public Result<List<NameCountVO>> typeRank(@RequestParam(defaultValue = "5") Integer limit) {
        return Result.success(statisticsService.typeRank(limit));
    }

    @GetMapping("/campusRank")
    @ApiOperation("各校区订单量分布")
    public Result<List<NameCountVO>> campusRank() {
        return Result.success(statisticsService.campusRank());
    }

    @GetMapping("/supplyDemand")
    @ApiOperation("类目供需缺口对比：在售服务数（供给）vs 近 30 天发单量（需求）+ 历史均价，按缺口排序")
    public Result<List<SupplyDemandVO>> supplyDemand() {
        return Result.success(statisticsService.supplyDemand());
    }
}
