package com.campus.runner.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 跑腿订单状态枚举
 */
public enum OrderStatus {
    PENDING_PAYMENT(1, "待支付"),
    TO_BE_TAKEN(2, "待接单"),
    IN_PROGRESS(3, "进行中"),
    DELIVERED(4, "已送达"),
    COMPLETED(5, "已完成"),
    CANCELLED(6, "已取消"),
    TIMEOUT(7, "已超时");

    private static final Map<Integer, OrderStatus> STATE_MAP = new HashMap<>();

    static {
        for (OrderStatus orderStatus : OrderStatus.values()) {
            STATE_MAP.put(orderStatus.getState(), orderStatus);
        }
    }

    private final Integer state;
    private final String desc;

    OrderStatus(Integer state, String desc) {
        this.state = state;
        this.desc = desc;
    }

    public Integer getState() {
        return state;
    }

    public String getDesc() {
        return desc;
    }

    public static OrderStatus fromState(Integer state) {
        return STATE_MAP.get(state);
    }
}
