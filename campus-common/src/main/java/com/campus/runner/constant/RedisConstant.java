package com.campus.runner.constant;

/**
 * Redis 缓存键常量
 */
public class RedisConstant {

    //营业状态 1营业中 0打烊中
    public static final String SHOP_STATUS_KEY = "campus:shop:status";

    //抢单分布式锁 key 前缀，完整键为 campus:order:grab:lock:{orderId}
    public static final String ORDER_GRAB_LOCK = "campus:order:grab:lock:";

    //跑腿员当日接单计数 key 前缀，完整键为 campus:runner:daily:count:{runnerId}:{yyyyMMdd}
    public static final String RUNNER_DAILY_COUNT = "campus:runner:daily:count:";

    //订单超时标记 key 前缀，完整键为 campus:order:timeout:{orderId}
    public static final String ORDER_TIMEOUT_KEY = "campus:order:timeout:";

    //抢单限流 key 前缀，完整键为 campus:order:grab:limit:{runnerId}
    public static final String RUNNER_GRAB_LIMIT = "campus:order:grab:limit:";
}
