package com.campus.runner.service;

import com.campus.runner.vo.OrderStatisticsVO;
import com.campus.runner.vo.RunnerStatisticsVO;

public interface StatisticsService {

    /**
     * 管理端-平台订单统计看板
     */
    OrderStatisticsVO overview();

    /**
     * 跑腿员端-个人统计
     */
    RunnerStatisticsVO runnerStats(Long runnerId);
}
