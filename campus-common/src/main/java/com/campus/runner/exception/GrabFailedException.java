package com.campus.runner.exception;

/**
 * 抢单失败异常（订单已被抢、达到接单上限、未认证等）
 */
public class GrabFailedException extends BaseException {

    public GrabFailedException() {
    }

    public GrabFailedException(String msg) {
        super(msg);
    }
}
