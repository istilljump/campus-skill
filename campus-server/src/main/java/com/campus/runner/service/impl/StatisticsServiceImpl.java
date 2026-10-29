package com.campus.runner.service.impl;

import com.campus.runner.mapper.OrderMapper;
import com.campus.runner.service.StatisticsService;
import com.campus.runner.vo.NameCountVO;
import com.campus.runner.vo.OrderStatisticsVO;
import com.campus.runner.vo.RunnerStatisticsVO;
import com.campus.runner.vo.SupplyDemandVO;
import com.campus.runner.vo.TrendPointVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private com.campus.runner.mapper.RunnerMapper runnerMapper;

    @Autowired
    private com.campus.runner.mapper.StatisticsMapper statisticsMapper;

    @Autowired
    private com.campus.runner.mapper.ReconcileMapper reconcileMapper;

    @Autowired
    private com.campus.runner.mapper.ReconcileRecordMapper reconcileRecordMapper;

    @Override
    public OrderStatisticsVO overview() {
        return orderMapper.statistics();
    }

    @Override
    public com.campus.runner.vo.TrustOverviewVO trustOverview() {
        //最近批次取自批次汇总表（0 差异轮也落行），避免"最近有差异批次"的过期口径
        Map<String, Object> batch = reconcileRecordMapper.latestBatch();
        String batchNo = batch != null ? String.valueOf(batch.get("batch_no")) : null;
        long mismatch = batch != null && batch.get("mismatch_count") != null ? ((Number) batch.get("mismatch_count")).longValue() : 0;
        long checked = batch != null && batch.get("checked_wallets") != null ? ((Number) batch.get("checked_wallets")).longValue() : reconcileMapper.countWallets();
        return com.campus.runner.vo.TrustOverviewVO.builder()
                .escrowAmount(statisticsMapper.sumEscrow())
                .walletBalance(statisticsMapper.sumWalletBalance())
                .withdrawPending(statisticsMapper.sumWithdrawPending())
                .riskUnhandled(statisticsMapper.countRiskUnhandled())
                .risk7d(statisticsMapper.countRisk7d())
                .reconcileBatchNo(batchNo)
                .reconcileMismatch(mismatch)
                .reconcileWallets(checked)
                .reconcileAllMatched(batch == null || mismatch == 0)
                .build();
    }

    @Override
    public java.util.List<com.campus.runner.vo.FunnelStageVO> funnel() {
        Map<String, Object> row = statisticsMapper.statFunnel();
        int published = ((Number) row.get("published")).intValue();
        int paid = nz(row.get("paid"));
        int accepted = nz(row.get("accepted"));
        int delivered = nz(row.get("delivered"));
        int completed = nz(row.get("completed"));
        return java.util.Arrays.asList(
                new com.campus.runner.vo.FunnelStageVO("发布订单", published),
                new com.campus.runner.vo.FunnelStageVO("完成支付", paid),
                new com.campus.runner.vo.FunnelStageVO("技能者接单", accepted),
                new com.campus.runner.vo.FunnelStageVO("提交交付", delivered),
                new com.campus.runner.vo.FunnelStageVO("验收完成", completed));
    }

    @Override
    public java.util.List<com.campus.runner.vo.NameCountVO> hourlyHeat(int days) {
        days = Math.max(1, Math.min(days, 30));
        Map<Integer, Integer> byHour = new HashMap<>();
        for (Map<String, Object> row : statisticsMapper.statHourly(LocalDateTime.now().minusDays(days))) {
            byHour.put(((Number) row.get("h")).intValue(), ((Number) row.get("c")).intValue());
        }
        java.util.List<com.campus.runner.vo.NameCountVO> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            result.add(new com.campus.runner.vo.NameCountVO(h + "时", byHour.getOrDefault(h, 0)));
        }
        return result;
    }

    private int nz(Object v) {
        return v == null ? 0 : ((Number) v).intValue();
    }

    @Override
    public RunnerStatisticsVO runnerStats(Long runnerId) {
        LocalDate today = LocalDate.now();
        LocalDate monthBegin = today.withDayOfMonth(1);
        com.campus.runner.entity.Runner runner = runnerMapper.getById(runnerId);
        return RunnerStatisticsVO.builder()
                .todayOrderCount(orderMapper.countRunnerOrdersBetween(runnerId, today, today))
                .todayIncome(orderMapper.sumRunnerIncomeBetween(runnerId, today, today))
                .monthOrderCount(orderMapper.countRunnerOrdersBetween(runnerId, monthBegin, today))
                .monthIncome(orderMapper.sumRunnerIncomeBetween(runnerId, monthBegin, today))
                .totalCompletedOrders(runner != null && runner.getCompletedOrders() != null ? runner.getCompletedOrders() : 0)
                .totalIncome(orderMapper.sumRunnerIncomeBetween(runnerId, LocalDate.of(2000, 1, 1), today))
                .score(runner != null ? runner.getScore() : BigDecimal.ZERO)
                .build();
    }

    @Override
    public List<TrendPointVO> dailyTrend(int days) {
        days = Math.max(1, Math.min(days, 90));
        LocalDate begin = LocalDate.now().minusDays(days - 1L);
        Map<String, Map<String, Object>> byDate = new HashMap<>();
        for (Map<String, Object> row : orderMapper.statDailyTrend(begin)) {
            byDate.put(String.valueOf(row.get("date")), row);
        }
        List<TrendPointVO> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String date = begin.plusDays(i).toString();
            Map<String, Object> row = byDate.get(date);
            result.add(TrendPointVO.builder()
                    .date(date)
                    .orderCount(row != null ? ((Number) row.get("orderCount")).intValue() : 0)
                    .completedCount(row != null && row.get("completedCount") != null
                            ? ((Number) row.get("completedCount")).intValue() : 0)
                    .amount(row != null && row.get("amount") != null
                            ? new BigDecimal(String.valueOf(row.get("amount"))) : BigDecimal.ZERO)
                    .build());
        }
        return result;
    }

    @Override
    public List<NameCountVO> typeRank(int limit) {
        limit = Math.max(1, Math.min(limit, 20));
        List<NameCountVO> result = new ArrayList<>();
        for (Map<String, Object> row : orderMapper.statTypeRank(limit)) {
            result.add(new NameCountVO(String.valueOf(row.get("name")), ((Number) row.get("count")).intValue()));
        }
        return result;
    }

    @Override
    public List<NameCountVO> campusRank() {
        List<NameCountVO> result = new ArrayList<>();
        for (Map<String, Object> row : orderMapper.statCampusRank()) {
            result.add(new NameCountVO(String.valueOf(row.get("name")), ((Number) row.get("count")).intValue()));
        }
        return result;
    }

    @Override
    public List<SupplyDemandVO> supplyDemand() {
        List<SupplyDemandVO> result = new ArrayList<>();
        for (Map<String, Object> row : orderMapper.statSupplyDemand(LocalDateTime.now().minusDays(30))) {
            result.add(SupplyDemandVO.builder()
                    .typeId(((Number) row.get("typeId")).longValue())
                    .typeName(String.valueOf(row.get("typeName")))
                    .supplyCount(((Number) row.get("supplyCount")).intValue())
                    .demandCount(((Number) row.get("demandCount")).intValue())
                    .avgPrice(row.get("avgPrice") != null
                            ? new BigDecimal(String.valueOf(row.get("avgPrice"))) : BigDecimal.ZERO)
                    .build());
        }
        return result;
    }
}
