package com.campus.runner.service;

import com.campus.runner.dto.OrdersBoostDTO;
import com.campus.runner.dto.OrdersCancelDTO;
import com.campus.runner.dto.OrdersGrabDTO;
import com.campus.runner.dto.OrdersPageQueryDTO;
import com.campus.runner.dto.OrdersSubmitDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.vo.OrderDetailVO;
import com.campus.runner.vo.OrderHallVO;
import com.campus.runner.vo.OrderSubmitVO;

public interface OrderService {

    /**
     * 用户端-发布订单
     */
    OrderSubmitVO submit(Long userId, OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 用户端-订单支付（微信为模拟支付，钱包为余额扣款）
     */
    void pay(String orderNumber, Long userId);

    /**
     * 用户端-订单追加悬赏（待接单且已支付，顺延超时时间）
     */
    void boost(Long userId, OrdersBoostDTO ordersBoostDTO);

    /**
     * 用户端-订单大厅查询（跑腿员端也复用）
     */
    PageResult<OrderHallVO> hallPage(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 用户端-我的订单分页
     */
    PageResult<OrderDetailVO> userPage(Long userId, OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 跑腿员端-接单记录分页
     */
    PageResult<OrderDetailVO> runnerPage(Long runnerId, OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 管理端-订单列表分页
     */
    PageResult<OrderDetailVO> adminPage(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 三端-订单详情
     */
    OrderDetailVO detail(Long orderId);

    /**
     * 跑腿员端-抢单（含并发控制）
     */
    void grab(Long runnerId, OrdersGrabDTO ordersGrabDTO);

    /**
     * 跑腿员端-确认取件
     */
    void pickup(Long orderId, Long runnerId);

    /**
     * 跑腿员端-确认送达（触发自动结算：平台服务费已扣、跑腿员到账、生成双方流水）
     */
    void deliver(Long orderId, Long runnerId);

    /**
     * 用户端-确认完成
     */
    void confirm(Long orderId, Long userId);

    /**
     * 用户端/跑腿员端-取消订单（cancelBy 1用户 2跑腿员）
     */
    void cancel(Long operatorId, Integer cancelBy, OrdersCancelDTO ordersCancelDTO);

    /**
     * 用户端-申诉
     */
    void appeal(Long orderId, Long userId);

    /**
     * 定时任务-超时订单处理（未支付超时取消、无人接单超时退款）
     */
    void processTimeoutOrders();
}
