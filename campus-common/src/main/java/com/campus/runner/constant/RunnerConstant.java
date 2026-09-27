package com.campus.runner.constant;

/**
 * 跑腿员业务常量
 */
public class RunnerConstant {

    /**
     * 认证状态
     */
    public static final Integer AUDIT_PENDING = 0;     //待认证
    public static final Integer AUDIT_PASSED = 1;      //已认证
    public static final Integer AUDIT_REJECTED = 2;    //认证拒绝
    public static final Integer AUDIT_REVIEWING = 3;   //认证审核中

    /**
     * 认证审核状态
     */
    public static final Integer REVIEW_PENDING = 0;    //待审核
    public static final Integer REVIEW_PASSED = 1;     //通过
    public static final Integer REVIEW_REJECTED = 2;   //驳回

    /**
     * 跑腿等级
     */
    public static final Integer LEVEL_NORMAL = 1;      //普通
    public static final Integer LEVEL_BRONZE = 2;      //铜牌
    public static final Integer LEVEL_SILVER = 3;      //银牌
    public static final Integer LEVEL_GOLD = 4;        //金牌

    /**
     * 订单取消方
     */
    public static final Integer CANCEL_BY_USER = 1;
    public static final Integer CANCEL_BY_RUNNER = 2;
    public static final Integer CANCEL_BY_PLATFORM = 3;

    //默认每日接单上限
    public static final int DEFAULT_DAILY_LIMIT = 10;
}
