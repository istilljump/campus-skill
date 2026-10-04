package com.campus.runner.service;

import com.campus.runner.vo.NameCountVO;
import com.campus.runner.vo.OrderStatisticsVO;
import com.campus.runner.vo.RunnerStatisticsVO;
import com.campus.runner.vo.TrendPointVO;

import java.util.List;

public interface StatisticsService {

    /**
     * 管理端-平台订单统计看板
     */
    OrderStatisticsVO overview();

    /**
     * 技能者端-个人统计
     */
    RunnerStatisticsVO runnerStats(Long runnerId);

    /**
     * 管理端-近 days 日订单趋势（缺失日期补零）
     */
    List<TrendPointVO> dailyTrend(int days);

    /**
     * 管理端-技能类型订单量 TOP N
     */
    List<NameCountVO> typeRank(int limit);

    /**
     * 管理端-各校区订单量分布
     */
    List<NameCountVO> campusRank();
}
