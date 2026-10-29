package com.campus.runner.constant;

/**
 * Spring Cache 缓存名常量（Redis 键前缀 = 缓存名 + ":"）。
 * 各缓存的 TTL 与失效策略见 RedisConfig.cacheManager。
 */
public class CacheNames {

    /**
     * 技能类目列表（TTL 10 分钟；管理端类目增删改时全量失效）
     */
    public static final String SKILL_CATEGORY = "cache:skillCategory";

    /**
     * 信用分排行榜（TTL 5 分钟；任何信用分变动时全量失效）
     */
    public static final String CREDIT_RANK = "cache:creditRank";

    /**
     * 服务货架分页（TTL 2 分钟；技能者上架/改价失效，成交销量变更时由订单结算主动失效）
     */
    public static final String MARKET_SHELF = "cache:marketShelf";

    /**
     * 服务货架详情（TTL 5 分钟；服务编辑/上下架/成交销量变更时失效）
     */
    public static final String SERVICE_DETAIL = "cache:serviceDetail";

    private CacheNames() {
    }
}
