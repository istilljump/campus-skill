package com.campus.runner.task;

import com.campus.runner.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 超时订单定时任务：未支付超时取消、无人接单超时退款
 */
@Component
@Slf4j
public class OrderTimeoutTask {

    @Autowired
    private OrderService orderService;

    //每分钟执行一次超时订单扫描
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrders() {
        log.info("定时扫描超时订单...");
        orderService.processTimeoutOrders();
    }
}
