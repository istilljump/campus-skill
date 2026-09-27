package com.campus.runner.service.impl;

import com.campus.runner.mapper.OrderMapper;
import com.campus.runner.service.StatisticsService;
import com.campus.runner.vo.OrderStatisticsVO;
import com.campus.runner.vo.RunnerStatisticsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}
