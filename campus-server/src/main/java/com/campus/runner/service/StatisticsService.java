package com.campus.runner.service;

import com.campus.runner.vo.NameCountVO;
import com.campus.runner.vo.OrderStatisticsVO;
import com.campus.runner.vo.SupplyDemandVO;
import com.campus.runner.vo.RunnerStatisticsVO;
import com.campus.runner.vo.TrendPointVO;

import java.util.List;

public interface StatisticsService {

    /**
     * 管理端-平台订单统计看板
     */
    OrderStatisticsVO overview();

    /**
     * 管理端-资金与信任总览（大屏 v2）：托管中/钱包余额/提现在途 + 风控告警 + 对账健康
     */
    com.campus.runner.vo.TrustOverviewVO trustOverview();

    /**
     * 管理端-交易转化漏斗：发布 → 支付 → 接单 → 交付 → 完成
     */
    java.util.List<com.campus.runner.vo.FunnelStageVO> funnel();

    /**
     * 管理端-订单发布时段热度（近 days 天，0-23 时补零）
     */
    java.util.List<com.campus.runner.vo.NameCountVO> hourlyHeat(int days);

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

    /**
     * 管理端-类目供需对比（在售服务数 vs 近 30 天发单量 + 历史均价），按缺口排序
     */
    List<SupplyDemandVO> supplyDemand();
}
