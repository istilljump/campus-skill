package com.campus.runner.service.impl;

import com.campus.runner.constant.RedisConstant;
import com.campus.runner.entity.Runner;
import com.campus.runner.entity.ServiceItem;
import com.campus.runner.mapper.OrderMapper;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.mapper.ServiceItemMapper;
import com.campus.runner.service.MarketInsightService;
import com.campus.runner.vo.PriceReferenceVO;
import com.campus.runner.vo.ServiceBriefVO;
import com.campus.runner.vo.SkillerCardVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 市场洞察实现：分位数在应用层计算（样本量为校园级数据量，无需数据库窗口函数），
 * 结果写入 Redis 短缓存，发单页高频打开时避免重复聚合。
 */
@Service
@Slf4j
public class MarketInsightServiceImpl implements MarketInsightService {

    /** 价格参考缓存时长：成交数据变化缓慢，5 分钟足够新 */
    private static final Duration PRICE_REF_TTL = Duration.ofMinutes(5);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Autowired
    private ServiceItemMapper serviceItemMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public PriceReferenceVO priceReference(Long typeId) {
        if (typeId == null) {
            return null;
        }
        String key = RedisConstant.MARKET_PRICE_REF + typeId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof PriceReferenceVO) {
                return (PriceReferenceVO) cached;
            }
        } catch (Exception e) {
            // Redis 未就绪时降级直查数据库，不影响发单主流程
            log.warn("读取价格参考缓存失败，降级直查数据库: {}", e.getMessage());
        }

        List<Double> amounts = new ArrayList<>();
        List<Double> hours = new ArrayList<>();
        for (Map<String, Object> row : orderMapper.statFinishedPricesByType(typeId)) {
            if (row.get("rewardAmount") != null) {
                amounts.add(new BigDecimal(String.valueOf(row.get("rewardAmount"))).doubleValue());
            }
            if (row.get("deliveryHours") != null) {
                hours.add(((Number) row.get("deliveryHours")).doubleValue());
            }
        }
        Collections.sort(amounts);
        Collections.sort(hours);

        PriceReferenceVO vo = PriceReferenceVO.builder()
                .sampleCount(amounts.size())
                .p25(toPlain(percentile(amounts, 25)))
                .p50(toPlain(percentile(amounts, 50)))
                .p75(toPlain(percentile(amounts, 75)))
                .avgDeliveryHours(round1(mean(hours)))
                .build();

        try {
            redisTemplate.opsForValue().set(key, vo, PRICE_REF_TTL);
        } catch (Exception e) {
            log.warn("写入价格参考缓存失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public SkillerCardVO skillerCard(Long runnerId) {
        if (runnerId == null) {
            return null;
        }
        Runner runner = runnerMapper.getById(runnerId);
        if (runner == null) {
            return null;
        }
        // 在售服务按销量取 Top3；未审核通过（status != ON_SHELF）的不出现
        List<ServiceBriefVO> services = serviceItemMapper.listBySkiller(runnerId).stream()
                .filter(s -> ServiceItem.ON_SHELF.equals(s.getStatus()))
                .sorted(Comparator.comparing(ServiceItem::getSalesCount,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .map(s -> ServiceBriefVO.builder()
                        .title(s.getTitle())
                        .price(s.getPrice())
                        .salesCount(s.getSalesCount())
                        .build())
                .toList();
        return SkillerCardVO.builder()
                .runnerId(runner.getId())
                .name(runner.getName())
                .runnerLevel(runner.getRunnerLevel())
                .score(runner.getScore())
                .creditScore(runner.getCreditScore())
                .completedOrders(runner.getCompletedOrders())
                .services(services)
                .build();
    }

    /** 最近秩分位数：p ∈ [0,100]，样本为空返回 0 */
    private static double percentile(List<Double> sorted, double p) {
        int n = sorted.size();
        if (n == 0) {
            return 0;
        }
        int idx = (int) Math.round(p / 100.0 * (n - 1));
        return sorted.get(Math.max(0, Math.min(n - 1, idx)));
    }

    private static double mean(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    private static BigDecimal toPlain(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static Double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
