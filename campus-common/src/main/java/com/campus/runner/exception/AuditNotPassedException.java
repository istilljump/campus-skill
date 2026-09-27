package com.campus.runner.exception;

/**
 * 认证未通过异常
 */
public class AuditNotPassedException extends BaseException {

    public AuditNotPassedException() {
    }

    public AuditNotPassedException(String msg) {
        super(msg);
    }
}
