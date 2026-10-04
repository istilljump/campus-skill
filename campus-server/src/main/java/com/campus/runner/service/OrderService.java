package com.campus.runner.service;

import com.campus.runner.dto.DisputeApplyDTO;
import com.campus.runner.dto.DisputeVerdictDTO;
import com.campus.runner.dto.OrdersBoostDTO;
import com.campus.runner.dto.OrdersCancelDTO;
import com.campus.runner.dto.OrdersDeliverDTO;
import com.campus.runner.dto.OrdersGrabDTO;
import com.campus.runner.dto.OrdersPageQueryDTO;
import com.campus.runner.dto.OrdersSubmitDTO;
import com.campus.runner.dto.ReworkDTO;
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
     * 用户端-订单大厅查询（技能者端也复用）
     */
    PageResult<OrderHallVO> hallPage(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 用户端-我的订单分页
     */
    PageResult<OrderDetailVO> userPage(Long userId, OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 技能者端-接单记录分页
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
     * 技能者端-抢单（含并发控制）
     */
    void grab(Long runnerId, OrdersGrabDTO ordersGrabDTO);

    /**
     * 技能者端-确认取件
     */
    void pickup(Long orderId, Long runnerId);

    /**
     * 技能者端-提交交付物（状态进行中→已交付，启动48小时自动验收倒计时，不结算）
     */
    void deliver(Long orderId, Long runnerId, OrdersDeliverDTO ordersDeliverDTO);

    /**
     * 用户端-验收订单（已交付→已完成，触发结算：报酬到账、完成数/等级/信用分联动）
     */
    void accept(Long orderId, Long userId);

    /**
     * 用户端-确认完成（兼容旧路由，等价于验收）
     */
    void confirm(Long orderId, Long userId);

    /**
     * 用户端-申请返修（已交付→返修中，技能者扣3信用分，最多2次）
     */
    void rework(Long orderId, Long userId, ReworkDTO reworkDTO);

    /**
     * 技能者端-响应返修（返修中→进行中，重新交付）
     */
    void respondRework(Long orderId, Long runnerId);

    /**
     * 用户端-发起仲裁（已交付/返修中→仲裁中）
     */
    void applyDispute(Long orderId, Long userId, DisputeApplyDTO disputeApplyDTO);

    /**
     * 管理端-仲裁判决（1退款用户 2放款技能者 3驳回）
     */
    void verdictDispute(Long disputeId, DisputeVerdictDTO disputeVerdictDTO, Long adminId);

    /**
     * 用户端/技能者端-取消订单（cancelBy 1用户 2技能者）
     */
    void cancel(Long operatorId, Integer cancelBy, OrdersCancelDTO ordersCancelDTO);

    /**
     * 用户端-申诉
     */
    void appeal(Long orderId, Long userId);

    /**
     * 定时任务-超时订单处理（未支付超时取消、无人接单超时退款、超时未交付处理、48小时自动验收）
     */
    void processTimeoutOrders();
}
