package com.campus.runner.exception;

/**
 * 余额不足异常
 */
public class InsufficientBalanceException extends BaseException {

    public InsufficientBalanceException() {
    }

    public InsufficientBalanceException(String msg) {
        super(msg);
    }
}
