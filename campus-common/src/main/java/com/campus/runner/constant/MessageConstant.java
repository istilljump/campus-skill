package com.campus.runner.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String ADDRESS_BOOK_IS_NULL = "请先添加送达地址";

    // ===== 订单相关 =====
    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_GRABBABLE = "手慢了，订单已被抢";
    public static final String ORDER_GRAB_FAILED = "抢单失败，请重试";
    public static final String GRAB_TOO_FREQUENT = "操作过于频繁，请稍后再试";
    public static final String ORDER_CANCEL_NOT_ALLOWED = "当前订单状态不可取消";
    public static final String ORDER_TIMEOUT = "订单已超时";
    public static final String ORDER_NOT_PAID = "订单尚未支付";
    public static final String REVIEW_EXISTS = "该订单已评价，请勿重复评价";
    public static final String REVIEW_NOT_ALLOWED = "订单完成后才能评价";
    public static final String CREDIT_TOO_LOW = "信用分不足，无法发单";
    public static final String APPEAL_SUBMITTED = "申诉已提交，请等待平台处理";

    // ===== 技能者相关 =====
    public static final String RUNNER_NOT_FOUND = "技能者不存在";
    public static final String RUNNER_NOT_CERTIFIED = "您尚未通过技能者认证";
    public static final String RUNNER_AUDIT_REVIEWING = "认证审核中，请耐心等待";
    public static final String AUDIT_NOT_PASSED = "认证未通过";
    public static final String AUDIT_NOT_FOUND = "审核记录不存在";
    public static final String AUDIT_ALREADY_PROCESSED = "该审核记录已处理";
    public static final String DAILY_LIMIT_REACHED = "已达到今日接单上限";
    public static final String WITHDRAW_PASSWORD_ERROR = "提现密码错误";
    public static final String WITHDRAW_PASSWORD_NOT_SET = "请先设置提现密码";
    public static final String WITHDRAW_NOT_FOUND = "提现申请不存在";
    public static final String WITHDRAW_ALREADY_PROCESSED = "该提现申请已处理";
    public static final String WITHDRAW_AMOUNT_INVALID = "提现金额不合法";
}
