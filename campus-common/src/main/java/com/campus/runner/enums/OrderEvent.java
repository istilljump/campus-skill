package com.campus.runner.enums;

/**
 * 技能订单事件（驱动订单状态流转）
 */
public enum OrderEvent {
    PAY,            // 支付成功 -> 待接单
    GRAB,           // 技能者抢单 -> 进行中
    PICKUP,         // 取件完成/配送中
    DELIVER,        // 送达 -> 已送达
    CONFIRM,        // 用户确认完成 -> 已完成
    USER_CANCEL,    // 用户取消
    RUNNER_CANCEL,  // 技能者取消
    PLATFORM_CANCEL,// 平台取消
    TIMEOUT,        // 超时
    APPEAL          // 申诉
}
