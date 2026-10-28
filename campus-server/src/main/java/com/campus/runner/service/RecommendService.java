package com.campus.runner.service;

/**
 * 个性化推荐引擎：用户行为采集 + 混合推荐（ItemCF 协同过滤 + 内容特征 + 质量因子）
 */
public interface RecommendService {

    /**
     * 记录用户对技能服务的行为（1浏览 2预约 3成交），source 标记行为来源
     * （1市场列表 2猜你喜欢 3看了又看 4其他，用于推荐效果对比实验）。失败只记日志，绝不影响主流程
     */
    void record(Long userId, Long serviceItemId, Integer behaviorType, Integer source);

    /**
     * 推荐效果对比：近 N 天各行为来源的浏览量与预约/成交转化率
     */
    java.util.List<com.campus.runner.vo.RecommendEffectVO> effectStats(Integer days);

    /**
     * 猜你喜欢：为用户推荐技能服务（含推荐理由）。冷启动回退热门精选
     */
    java.util.List<com.campus.runner.vo.ServiceItemVO> recommendForUser(Long userId, Integer limit);

    /**
     * 看了又看：与指定服务相似的其他服务（含推荐理由）
     */
    java.util.List<com.campus.runner.vo.ServiceItemVO> similarItems(Long serviceItemId, Integer limit);
}
