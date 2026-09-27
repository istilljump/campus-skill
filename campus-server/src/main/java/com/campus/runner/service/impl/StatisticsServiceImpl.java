package com.campus.runner.service.impl;

import com.campus.runner.mapper.OrderMapper;
import com.campus.runner.service.StatisticsService;
import com.campus.runner.vo.NameCountVO;
import com.campus.runner.vo.OrderStatisticsVO;
import com.campus.runner.vo.RunnerStatisticsVO;
import com.campus.runner.vo.TrendPointVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Override
    public OrderStatisticsVO overview() {
        return orderMapper.statistics();
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
}
