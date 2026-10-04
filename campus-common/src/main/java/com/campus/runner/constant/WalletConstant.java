package com.campus.runner.constant;

/**
 * 钱包业务常量
 */
public class WalletConstant {

    /**
     * 钱包流水类型
     */
    public static final Integer TYPE_INCOME = 1;      //技能收入
    public static final Integer TYPE_EXPENSE = 2;     //支付支出
    public static final Integer TYPE_RECHARGE = 3;    //充值
    public static final Integer TYPE_WITHDRAW = 4;    //提现
    public static final Integer TYPE_REFUND = 5;      //退款
    public static final Integer TYPE_PENALTY = 6;     //违约金

    /**
     * 钱包账户状态
     */
    public static final Integer ACCOUNT_FROZEN = 0;
    public static final Integer ACCOUNT_NORMAL = 1;

    /**
     * 提现申请状态
     */
    public static final Integer WITHDRAW_PENDING = 0;   //待处理
    public static final Integer WITHDRAW_PAID = 1;      //已打款
    public static final Integer WITHDRAW_REJECTED = 2;  //已驳回
}
