package com.campus.runner.service;

import com.campus.runner.vo.PriceReferenceVO;
import com.campus.runner.vo.SkillerCardVO;

/**
 * 市场洞察：数据驱动的发单辅助（价格分位参考等）
 */
public interface MarketInsightService {

    /**
     * 类目历史成交价参考（P25/P50/P75 分位 + 平均交付时长，Redis 缓存 5 分钟）
     *
     * @param typeId 技能类目 ID
     * @return 价格参考；类目无历史成交时各项为 0/空样本量
     */
    PriceReferenceVO priceReference(Long typeId);

    /**
     * 技能者分享名片（公开脱敏：姓名/等级/评分/信用 + 在售服务 Top3，供扫码落地页与海报使用）
     *
     * @param runnerId 技能者 ID
     * @return 名片数据；技能者不存在时返回 null
     */
    SkillerCardVO skillerCard(Long runnerId);
}
