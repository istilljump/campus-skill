package com.campus.runner.service.impl;

import com.campus.runner.entity.ServiceItem;
import com.campus.runner.entity.UserBehavior;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.mapper.ServiceItemMapper;
import com.campus.runner.mapper.UserBehaviorMapper;
import com.campus.runner.service.RecommendService;
import com.campus.runner.vo.ServiceItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 个性化推荐引擎实现
 *
 * <p>混合推荐打分：score = 0.45 * 协同过滤分 + 0.30 * 内容特征分 + 0.25 * 质量分
 * <ul>
 *   <li>协同过滤（ItemCF）：基于全体用户近 90 天「用户-服务」加权行为矩阵，计算服务间余弦相似度，
 *       再用目标用户的历史加权行为预测其对候选服务的兴趣</li>
 *   <li>内容特征：类目偏好（用户历史行为类目占比）+ 标签 Jaccard 相似度 + 价格贴近度</li>
 *   <li>质量因子：销量、服务均分、技能者信用分与作品定级，保证推荐结果的基础可信度；
 *       同时作为无行为用户（冷启动）的兜底排序依据</li>
 * </ul>
 */
@Service
@Slf4j
public class RecommendServiceImpl implements RecommendService {

    /** 行为权重：浏览 1.0 / 预约 3.0 / 成交 5.0 */
    private static final BigDecimal WEIGHT_VIEW = new BigDecimal("1.0");
    private static final BigDecimal WEIGHT_BOOKING = new BigDecimal("3.0");
    private static final BigDecimal WEIGHT_DEAL = new BigDecimal("5.0");

    /** 三路分数权重 */
    private static final double W_CF = 0.45;
    private static final double W_CONTENT = 0.30;
    private static final double W_QUALITY = 0.25;

    /** 浏览行为 30 分钟内去重，避免刷浏览记录 */
    private static final int VIEW_DEDUPE_MINUTES = 30;
    private static final int USER_HISTORY_LIMIT = 50;
    private static final int CF_MATRIX_WINDOW_DAYS = 90;

    @Autowired
    private UserBehaviorMapper userBehaviorMapper;

    @Autowired
    private ServiceItemMapper serviceItemMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Override
    public void record(Long userId, Long serviceItemId, Integer behaviorType, Integer source) {
        try {
            if (userId == null || serviceItemId == null) {
                return;
            }
            //浏览行为 30 分钟窗口内去重
            if (UserBehavior.TYPE_VIEW.equals(behaviorType)) {
                boolean exists = userBehaviorMapper.listRecentDistinctByUser(userId, USER_HISTORY_LIMIT).stream()
                        .anyMatch(b -> b.getServiceItemId().equals(serviceItemId)
                                && UserBehavior.TYPE_VIEW.equals(b.getBehaviorType())
                                && b.getCreateTime().isAfter(LocalDateTime.now().minusMinutes(VIEW_DEDUPE_MINUTES)));
                if (exists) {
                    return;
                }
            }
            UserBehavior behavior = UserBehavior.builder()
                    .userId(userId)
                    .serviceItemId(serviceItemId)
                    .behaviorType(behaviorType)
                    .source(source == null ? UserBehavior.SOURCE_OTHER : source)
                    .weight(weightOf(behaviorType))
                    .createTime(LocalDateTime.now())
                    .build();
            ServiceItem item = serviceItemMapper.getById(serviceItemId);
            if (item != null) {
                behavior.setCategoryId(item.getCategoryId());
            }
            userBehaviorMapper.insert(behavior);
        } catch (Exception e) {
            //行为采集属于旁路逻辑，失败不影响主业务
            log.warn("用户行为记录失败 userId={}, serviceItemId={}, type={}", userId, serviceItemId, behaviorType, e);
        }
    }

    @Override
    public List<com.campus.runner.vo.RecommendEffectVO> effectStats(Integer days) {
        int window = days == null || days <= 0 ? 7 : days;
        List<Map<String, Object>> rows = userBehaviorMapper.countBySource(window);
        Map<Integer, long[]> bySource = new HashMap<>();
        for (Map<String, Object> row : rows) {
            int source = ((Number) row.get("source")).intValue();
            int type = ((Number) row.get("behaviorType")).intValue();
            long cnt = ((Number) row.get("cnt")).longValue();
            long[] agg = bySource.computeIfAbsent(source, k -> new long[3]);
            if (type == UserBehavior.TYPE_VIEW) {
                agg[0] += cnt;
            }
            if (type == UserBehavior.TYPE_BOOKING || type == UserBehavior.TYPE_DEAL) {
                agg[1] += cnt;
            }
        }
        List<com.campus.runner.vo.RecommendEffectVO> result = new ArrayList<>();
        for (Map.Entry<Integer, long[]> e : bySource.entrySet()) {
            long views = e.getValue()[0];
            long conversions = e.getValue()[1];
            result.add(com.campus.runner.vo.RecommendEffectVO.builder()
                    .source(e.getKey())
                    .sourceName(sourceName(e.getKey()))
                    .views(views)
                    .conversions(conversions)
                    .ctr(views == 0 ? BigDecimal.ZERO
                            : BigDecimal.valueOf(conversions * 100.0 / views).setScale(1, RoundingMode.HALF_UP))
                    .build());
        }
        result.sort(Comparator.comparing(com.campus.runner.vo.RecommendEffectVO::getSource));
        return result;
    }

    private String sourceName(Integer source) {
        if (UserBehavior.SOURCE_MARKET.equals(source)) {
            return "市场列表";
        }
        if (UserBehavior.SOURCE_RECOMMEND.equals(source)) {
            return "猜你喜欢";
        }
        if (UserBehavior.SOURCE_SIMILAR.equals(source)) {
            return "看了又看";
        }
        return "其他";
    }

    @Override
    public List<ServiceItemVO> recommendForUser(Long userId, Integer limit) {
        int topN = normalizeLimit(limit);
        List<ServiceItemVO> candidates = serviceItemMapper.listOnShelf(null, null, "default");
        if (candidates.isEmpty()) {
            return candidates;
        }
        List<UserBehavior> history = userBehaviorMapper.listRecentDistinctByUser(userId, USER_HISTORY_LIMIT);

        if (history.isEmpty()) {
            //冷启动：无行为数据，回退质量分排序（热门精选）
            List<ServiceItemVO> result = new ArrayList<>(candidates);
            result.sort(Comparator.comparingDouble(this::qualityScore).reversed());
            return result.stream().limit(topN).peek(vo ->
                    vo.setRecommendReason(String.format("热门精选 · 已售%d · 均分%s", vo.getSalesCount(), vo.getAvgScore())))
                    .collect(Collectors.toList());
        }

        //构建 ItemCF 相似度矩阵与用户兴趣画像
        Map<Long, Map<Long, Double>> itemVectors = buildItemVectors();
        Map<Long, Double> categoryAffinity = buildCategoryAffinity(history);
        Map<Long, ServiceItemVO> candidateMap = candidates.stream()
                .collect(Collectors.toMap(ServiceItemVO::getId, v -> v, (a, b) -> a));
        double avgHistoryPrice = history.stream()
                .map(b -> candidateMap.get(b.getServiceItemId()))
                .filter(v -> v != null)
                .mapToDouble(v -> v.getPrice().doubleValue())
                .average().orElse(0);

        //排除用户自己（作为技能者）挂牌的服务，避免「自己推荐给自己」
        Map<Long, Long> skillerUserIds = candidates.stream()
                .map(ServiceItemVO::getSkillerId).distinct()
                .collect(Collectors.toMap(sid -> sid, sid -> {
                    com.campus.runner.entity.Runner skiller = runnerMapper.getById(sid);
                    return skiller == null || skiller.getUserId() == null ? -1L : skiller.getUserId();
                }, (a, b) -> a));
        Set<Long> ownItemIds = candidates.stream()
                .filter(v -> userId.equals(skillerUserIds.getOrDefault(v.getSkillerId(), -1L)))
                .map(ServiceItemVO::getId).collect(Collectors.toSet());

        //已成交（type=3）的服务不再重复推荐；仅浏览过的服务降权
        Set<Long> dealtItems = history.stream()
                .filter(b -> UserBehavior.TYPE_DEAL.equals(b.getBehaviorType()))
                .map(UserBehavior::getServiceItemId).collect(Collectors.toSet());
        Set<Long> viewedItems = history.stream().map(UserBehavior::getServiceItemId).collect(Collectors.toSet());

        List<ScoredItem> ranked = new ArrayList<>();
        for (ServiceItemVO vo : candidates) {
            if (dealtItems.contains(vo.getId()) || ownItemIds.contains(vo.getId())) {
                continue;
            }
            double cf = cfScore(vo, history, itemVectors);
            double content = contentScore(vo, candidateMap, history, categoryAffinity, avgHistoryPrice);
            double quality = qualityScore(vo);
            double total = W_CF * cf + W_CONTENT * content + W_QUALITY * quality;
            if (viewedItems.contains(vo.getId())) {
                total *= 0.6; //近期已浏览过的服务降权，把曝光位让给新内容
            }
            ranked.add(new ScoredItem(vo, total, cf, content, quality, categoryAffinity));
        }
        ranked.sort(Comparator.comparingDouble(ScoredItem::getTotal).reversed());
        return ranked.stream().limit(topN).map(s -> {
            s.getVo().setRecommendReason(buildReason(s));
            return s.getVo();
        }).collect(Collectors.toList());
    }

    @Override
    public List<ServiceItemVO> similarItems(Long serviceItemId, Integer limit) {
        int topN = normalizeLimit(limit);
        ServiceItemVO base = serviceItemMapper.getVOById(serviceItemId);
        if (base == null) {
            return new ArrayList<>();
        }
        List<ServiceItemVO> candidates = serviceItemMapper.listOnShelf(null, null, "default");
        Map<Long, Map<Long, Double>> itemVectors = buildItemVectors();
        Set<String> baseTags = splitTags(base.getTags());
        double basePrice = base.getPrice().doubleValue();

        List<ScoredItem> ranked = new ArrayList<>();
        for (ServiceItemVO vo : candidates) {
            if (vo.getId().equals(serviceItemId)) {
                continue;
            }
            double sim = contentSimilarity(baseTags, basePrice, base.getCategoryId(), vo);
            //协同信号：与其他用户共同行为过的服务加成
            Map<Long, Double> vecB = itemVectors.get(serviceItemId);
            Map<Long, Double> vecC = itemVectors.get(vo.getId());
            if (vecB != null && vecC != null) {
                sim += 0.15 * cosine(vecB, vecC);
            }
            ranked.add(new ScoredItem(vo, sim, 0, sim, 0, Map.of()));
        }
        ranked.sort(Comparator.comparingDouble(ScoredItem::getTotal).reversed());
        return ranked.stream().limit(topN).map(s -> {
            s.getVo().setRecommendReason(buildSimilarReason(s.getVo(), base));
            return s.getVo();
        }).collect(Collectors.toList());
    }

    // ==================== 打分因子 ====================

    /**
     * ItemCF 兴趣分：Σ(历史行为权重 × sim(历史服务, 候选服务)) / Σsim
     */
    private double cfScore(ServiceItemVO candidate, List<UserBehavior> history, Map<Long, Map<Long, Double>> itemVectors) {
        Map<Long, Double> candVec = itemVectors.get(candidate.getId());
        if (candVec == null || candVec.isEmpty()) {
            return 0;
        }
        double num = 0;
        double den = 0;
        for (UserBehavior b : history) {
            if (b.getServiceItemId().equals(candidate.getId())) {
                continue;
            }
            Map<Long, Double> histVec = itemVectors.get(b.getServiceItemId());
            if (histVec == null) {
                continue;
            }
            double sim = cosine(histVec, candVec);
            num += b.getWeight().doubleValue() * sim;
            den += sim;
        }
        if (den <= 1e-9) {
            return 0;
        }
        return num / den / WEIGHT_DEAL.doubleValue(); //归一化到 0~1
    }

    /**
     * 内容特征分：类目偏好 + 标签 Jaccard + 价格贴近度
     */
    private double contentScore(ServiceItemVO vo, Map<Long, ServiceItemVO> candidateMap, List<UserBehavior> history,
                                Map<Long, Double> categoryAffinity, double avgHistoryPrice) {
        double category = categoryAffinity.getOrDefault(vo.getCategoryId(), 0.0);
        //标签：与用户历史行为过的服务求最大 Jaccard
        Set<String> voTags = splitTags(vo.getTags());
        double tag = 0;
        for (UserBehavior b : history) {
            ServiceItemVO viewed = candidateMap.get(b.getServiceItemId());
            if (viewed != null) {
                tag = Math.max(tag, jaccard(splitTags(viewed.getTags()), voTags));
            }
        }
        double price = 1;
        if (avgHistoryPrice > 0 && vo.getPrice() != null) {
            price = 1 - Math.min(1, Math.abs(vo.getPrice().doubleValue() - avgHistoryPrice) / avgHistoryPrice);
        }
        return 0.5 * category + 0.3 * tag + 0.2 * price;
    }

    /**
     * 质量分：销量 + 服务均分 + 技能者信用/定级
     */
    private double qualityScore(ServiceItemVO vo) {
        int sales = vo.getSalesCount() == null ? 0 : vo.getSalesCount();
        double salesNorm = Math.min(1, sales / 30.0);
        double score = vo.getAvgScore() == null ? 0 : vo.getAvgScore().doubleValue() / 5.0;
        double credit = vo.getSkillerCreditScore() == null ? 0 : Math.min(1, vo.getSkillerCreditScore() / 120.0);
        double level = vo.getSkillerSkillLevel() == null ? 0 : vo.getSkillerSkillLevel() / 3.0;
        return 0.4 * salesNorm + 0.3 * score + 0.2 * credit + 0.1 * level;
    }

    /**
     * 两个服务的内容相似度：同类目 + 标签 Jaccard + 价格贴近度
     */
    private double contentSimilarity(Set<String> baseTags, double basePrice, Long baseCategoryId, ServiceItemVO vo) {
        double category = baseCategoryId != null && baseCategoryId.equals(vo.getCategoryId()) ? 1 : 0;
        double tag = jaccard(baseTags, splitTags(vo.getTags()));
        double price = 1;
        if (basePrice > 0 && vo.getPrice() != null) {
            price = 1 - Math.min(1, Math.abs(vo.getPrice().doubleValue() - basePrice) / basePrice);
        }
        return 0.4 * category + 0.4 * tag + 0.2 * price;
    }

    // ==================== 矩阵与画像构建 ====================

    /**
     * 近 90 天全量「用户-服务」加权行为矩阵：itemId -> (userId -> weight)
     */
    private Map<Long, Map<Long, Double>> buildItemVectors() {
        List<UserBehavior> all = userBehaviorMapper.listRecentDistinctAll(CF_MATRIX_WINDOW_DAYS);
        Map<Long, Map<Long, Double>> vectors = new HashMap<>();
        for (UserBehavior b : all) {
            vectors.computeIfAbsent(b.getServiceItemId(), k -> new HashMap<>())
                    .merge(b.getUserId(), b.getWeight().doubleValue(), Math::max);
        }
        return vectors;
    }

    /**
     * 用户类目偏好画像：各类目累计行为权重 / 总权重
     */
    private Map<Long, Double> buildCategoryAffinity(List<UserBehavior> history) {
        Map<Long, Double> affinity = new LinkedHashMap<>();
        double total = history.stream().mapToDouble(b -> b.getWeight().doubleValue()).sum();
        if (total <= 0) {
            return affinity;
        }
        for (UserBehavior b : history) {
            if (b.getCategoryId() != null) {
                affinity.merge(b.getCategoryId(), b.getWeight().doubleValue() / total, Double::sum);
            }
        }
        return affinity;
    }

    // ==================== 工具方法 ====================

    private BigDecimal weightOf(Integer behaviorType) {
        if (UserBehavior.TYPE_DEAL.equals(behaviorType)) {
            return WEIGHT_DEAL;
        }
        if (UserBehavior.TYPE_BOOKING.equals(behaviorType)) {
            return WEIGHT_BOOKING;
        }
        return WEIGHT_VIEW;
    }

    private double cosine(Map<Long, Double> a, Map<Long, Double> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        double dot = 0;
        for (Map.Entry<Long, Double> e : a.entrySet()) {
            Double other = b.get(e.getKey());
            if (other != null) {
                dot += e.getValue() * other;
            }
        }
        double normA = Math.sqrt(a.values().stream().mapToDouble(v -> v * v).sum());
        double normB = Math.sqrt(b.values().stream().mapToDouble(v -> v * v).sum());
        if (normA <= 1e-9 || normB <= 1e-9) {
            return 0;
        }
        return dot / (normA * normB);
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) inter.size() / union.size();
    }

    private Set<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return new HashSet<>();
        }
        Set<String> result = new HashSet<>();
        for (String t : tags.split("[,，;；]")) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 8;
        }
        return Math.min(limit, 20);
    }

    /**
     * 推荐理由：取三路因子中贡献最大的一路生成人类可读文案
     */
    private String buildReason(ScoredItem s) {
        ServiceItemVO vo = s.getVo();
        if (s.getCf() >= s.getContent() && s.getCf() >= s.getQuality() && s.getCf() > 0) {
            return "与你互动过的服务相似";
        }
        if (s.getContent() >= s.getQuality() && s.getCategoryAffinity().getOrDefault(vo.getCategoryId(), 0.0) > 0.4) {
            return "你常看「" + (vo.getCategoryName() == null ? "同类" : vo.getCategoryName()) + "」技能";
        }
        if (s.getQuality() > 0) {
            return String.format("同类热门 · 已售%d · 均分%s", vo.getSalesCount(), vo.getAvgScore());
        }
        return "为你挑选";
    }

    private String buildSimilarReason(ServiceItemVO vo, ServiceItemVO base) {
        if (base.getCategoryId() != null && base.getCategoryId().equals(vo.getCategoryId())) {
            return "同属「" + (vo.getCategoryName() == null ? "同类" : vo.getCategoryName()) + "」技能";
        }
        if (!splitTags(base.getTags()).isEmpty() && jaccard(splitTags(base.getTags()), splitTags(vo.getTags())) > 0) {
            return "标签相近";
        }
        return "价格相近";
    }

    /**
     * 打分中间结果（含三路因子，用于生成推荐理由）
     */
    private static class ScoredItem {
        final ServiceItemVO vo;
        final double total;
        final double cf;
        final double content;
        final double quality;
        final Map<Long, Double> categoryAffinity;

        ScoredItem(ServiceItemVO vo, double total, double cf, double content, double quality,
                   Map<Long, Double> categoryAffinity) {
            this.vo = vo;
            this.total = total;
            this.cf = cf;
            this.content = content;
            this.quality = quality;
            this.categoryAffinity = categoryAffinity;
        }

        double getTotal() {
            return total;
        }

        ServiceItemVO getVo() {
            return vo;
        }

        double getCf() {
            return cf;
        }

        double getContent() {
            return content;
        }

        double getQuality() {
            return quality;
        }

        Map<Long, Double> getCategoryAffinity() {
            return categoryAffinity;
        }
    }
}
