package com.campus.runner.service.impl;

import com.campus.runner.constant.MessageConstant;
import com.campus.runner.constant.RedisConstant;
import com.campus.runner.constant.RunnerConstant;
import com.campus.runner.dto.OrdersCancelDTO;
import com.campus.runner.dto.OrdersGrabDTO;
import com.campus.runner.dto.OrdersPageQueryDTO;
import com.campus.runner.dto.OrdersSubmitDTO;
import com.campus.runner.entity.AddressBook;
import com.campus.runner.entity.ErrandType;
import com.campus.runner.entity.Orders;
import com.campus.runner.entity.Runner;
import com.campus.runner.entity.User;
import com.campus.runner.exception.BusinessException;
import com.campus.runner.exception.GrabFailedException;
import com.campus.runner.exception.OrderBusinessException;
import com.campus.runner.mapper.AddressBookMapper;
import com.campus.runner.mapper.ErrandTypeMapper;
import com.campus.runner.mapper.OrderMapper;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.mapper.UserMapper;
import com.campus.runner.result.PageResult;
import com.campus.runner.service.OrderService;
import com.campus.runner.service.WalletService;
import com.campus.runner.vo.OrderDetailVO;
import com.campus.runner.vo.OrderHallVO;
import com.campus.runner.vo.OrderSubmitVO;
import com.campus.runner.vo.OrderStatisticsVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    //信用分门槛：低于该值不允许发单
    private static final int MIN_CREDIT_SCORE = 60;

    //抢单限流：10 秒窗口内最大请求数
    private static final int GRAB_RATE_LIMIT = 5;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ErrandTypeMapper errandTypeMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Autowired
    private WalletService walletService;

    @Autowired
    private com.campus.runner.service.RunnerService runnerService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submit(Long userId, OrdersSubmitDTO dto) {
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new BusinessException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (user.getCreditScore() != null && user.getCreditScore() < MIN_CREDIT_SCORE) {
            throw new BusinessException(MessageConstant.CREDIT_TOO_LOW);
        }

        ErrandType type = errandTypeMapper.getById(dto.getTypeId());
        if (type == null || type.getStatus() == null || type.getStatus() != 1) {
            throw new BusinessException("订单类型不存在或已停用");
        }

        //送达地址：优先使用地址簿快照，保证订单不因地址变更失真
        String deliveryAddress = dto.getDeliveryAddress();
        String campus = dto.getCampus();
        if (dto.getAddressBookId() != null) {
            AddressBook addressBook = addressBookMapper.getById(dto.getAddressBookId());
            if (addressBook == null || !addressBook.getUserId().equals(userId)) {
                throw new BusinessException("送达地址不存在");
            }
            deliveryAddress = addressBook.detailedAddress();
            campus = addressBook.getCampus();
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            throw new BusinessException("送达地址不能为空");
        }

        //平台服务费按类型费率从悬赏中扣除，跑腿员实得 = 悬赏 - 服务费
        BigDecimal reward = dto.getRewardAmount();
        BigDecimal platformFee = reward.multiply(type.getFeeRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal runnerIncome = reward.subtract(platformFee);

        LocalDateTime now = LocalDateTime.now();
        Orders order = Orders.builder()
                .number(generateOrderNumber())
                .userId(userId)
                .typeId(dto.getTypeId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .pickupAddress(dto.getPickupAddress())
                .deliveryAddress(deliveryAddress)
                .campus(campus)
                .rewardAmount(reward)
                .platformFee(platformFee)
                .runnerIncome(runnerIncome)
                .status(Orders.PENDING_PAYMENT)
                .expectedTime(dto.getExpectedTime())
                //未填期望时间时默认2小时超时
                .timeoutTime(dto.getExpectedTime() != null ? dto.getExpectedTime() : now.plusHours(2))
                .payMethod(dto.getPayMethod() == null ? 1 : dto.getPayMethod())
                .payStatus(Orders.UN_PAID)
                .orderTime(now)
                .createTime(now)
                .updateTime(now)
                .build();
        orderMapper.insert(order);
        userMapper.incrementPublishOrderCount(userId);

        return OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .rewardAmount(reward)
                .platformFee(platformFee)
                .payAmount(reward)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(String orderNumber, Long userId) {
        Orders order = orderMapper.getByNumber(orderNumber);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != Orders.PENDING_PAYMENT || order.getPayStatus() != Orders.UN_PAID) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //钱包余额支付：扣款并记录支出流水
        if (order.getPayMethod() != null && order.getPayMethod() == 2) {
            walletService.changeBalance(userId, order.getRewardAmount().negate(),
                    com.campus.runner.constant.WalletConstant.TYPE_EXPENSE, order.getId(), "跑腿订单支付-" + order.getNumber());
        }
        Orders upd = Orders.builder().id(order.getId())
                .payStatus(Orders.PAID)
                .payTime(LocalDateTime.now())
                .status(Orders.TO_BE_TAKEN)
                .build();
        orderMapper.update(upd);
        log.info("订单支付成功，orderNumber={}, userId={}", orderNumber, userId);
    }

    @Override
    public PageResult<OrderHallVO> hallPage(OrdersPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<OrderHallVO> page = orderMapper.pageHallOrders(dto);
        return new PageResult<>(page.getTotal(), page.getResult(), page.getPageSize(), page.getPageNum());
    }

    @Override
    public PageResult<OrderDetailVO> userPage(Long userId, OrdersPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<Orders> page = orderMapper.pageUserOrders(userId, dto);
        List<OrderDetailVO> vos = page.getResult().stream().map(o -> detail(o.getId())).toList();
        return new PageResult<>(page.getTotal(), vos, page.getPageSize(), page.getPageNum());
    }

    @Override
    public PageResult<OrderDetailVO> runnerPage(Long runnerId, OrdersPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<Orders> page = orderMapper.pageRunnerOrders(runnerId, dto);
        List<OrderDetailVO> vos = page.getResult().stream().map(o -> detail(o.getId())).toList();
        return new PageResult<>(page.getTotal(), vos, page.getPageSize(), page.getPageNum());
    }

    @Override
    public PageResult<OrderDetailVO> adminPage(OrdersPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<Orders> page = orderMapper.pageAdminOrders(dto);
        List<OrderDetailVO> vos = page.getResult().stream().map(o -> detail(o.getId())).toList();
        return new PageResult<>(page.getTotal(), vos, page.getPageSize(), page.getPageNum());
    }

    @Override
    public OrderDetailVO detail(Long orderId) {
        Orders order = orderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        OrderDetailVO vo = OrderDetailVO.builder()
                .id(order.getId())
                .number(order.getNumber())
                .status(order.getStatus())
                .title(order.getTitle())
                .description(order.getDescription())
                .typeId(order.getTypeId())
                .pickupAddress(order.getPickupAddress())
                .deliveryAddress(order.getDeliveryAddress())
                .campus(order.getCampus())
                .rewardAmount(order.getRewardAmount())
                .platformFee(order.getPlatformFee())
                .runnerIncome(order.getRunnerIncome())
                .expectedTime(order.getExpectedTime())
                .timeoutTime(order.getTimeoutTime())
                .cancelReason(order.getCancelReason())
                .cancelBy(order.getCancelBy())
                .isAppealed(order.getIsAppealed())
                .payStatus(order.getPayStatus())
                .orderTime(order.getOrderTime())
                .payTime(order.getPayTime())
                .finishTime(order.getFinishTime())
                .createTime(order.getCreateTime())
                .build();
        if (order.getTypeId() != null) {
            ErrandType type = errandTypeMapper.getById(order.getTypeId());
            vo.setTypeName(type != null ? type.getName() : null);
        }
        User publisher = userMapper.getById(order.getUserId());
        if (publisher != null) {
            vo.setPublisherName(maskName(publisher.getName()));
            vo.setPublisherPhone(publisher.getPhone());
        }
        if (order.getRunnerId() != null) {
            Runner runner = runnerMapper.getById(order.getRunnerId());
            if (runner != null) {
                vo.setRunnerName(runner.getName());
                vo.setRunnerPhone(runner.getPhone());
                vo.setRunnerScore(runner.getScore());
            }
        }
        return vo;
    }

    @Override
    public void grab(Long runnerId, OrdersGrabDTO dto) {
        //接单权限校验：认证状态、账号状态、每日接单上限
        checkGrabPermission(runnerId, dto.getId());
        Long orderId = dto.getId();
        //限流：同一跑腿员 10 秒内最多 5 次抢单请求，防止刷接口
        String limitKey = RedisConstant.RUNNER_GRAB_LIMIT + runnerId;
        Long count = redisTemplate.opsForValue().increment(limitKey);
        if (count != null && count == 1) {
            redisTemplate.expire(limitKey, 10, TimeUnit.SECONDS);
        }
        if (count != null && count > GRAB_RATE_LIMIT) {
            throw new GrabFailedException(MessageConstant.GRAB_TOO_FREQUENT);
        }
        String lockKey = RedisConstant.ORDER_GRAB_LOCK + orderId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, runnerId, 10, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new GrabFailedException(MessageConstant.ORDER_GRAB_FAILED);
        }
        try {
            //条件更新：仅当订单仍为待接单且已支付时生效，数据库层面保证不重复抢单
            int rows = orderMapper.grabOrder(orderId, runnerId);
            if (rows == 0) {
                throw new GrabFailedException(MessageConstant.ORDER_NOT_GRABBABLE);
            }
            log.info("抢单成功，orderId={}, runnerId={}", orderId, runnerId);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickup(Long orderId, Long runnerId) {
        Orders order = getOwnedOrder(orderId, runnerId);
        if (order.getStatus() != Orders.IN_PROGRESS) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders upd = Orders.builder().id(order.getId())
                .pickupTime(LocalDateTime.now())
                .build();
        orderMapper.update(upd);
        log.info("确认取件，orderId={}, runnerId={}", orderId, runnerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliver(Long orderId, Long runnerId) {
        Orders order = getOwnedOrder(orderId, runnerId);
        if (order.getStatus() != Orders.IN_PROGRESS) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //自动结算：跑腿员到账 = 悬赏 - 平台服务费，生成双方流水
        Runner runner = runnerMapper.getById(runnerId);
        if (runner == null) {
            throw new OrderBusinessException(MessageConstant.RUNNER_NOT_FOUND);
        }
        walletService.changeBalance(runner.getUserId(), order.getRunnerIncome(),
                com.campus.runner.constant.WalletConstant.TYPE_INCOME, order.getId(),
                "跑腿订单收入-" + order.getNumber());
        //累计完成订单数并自动更新跑腿等级
        runnerMapper.incrementCompletedOrders(runnerId);
        runnerService.updateLevel(runnerId);
        Orders upd = Orders.builder().id(order.getId())
                .status(Orders.DELIVERED)
                .build();
        orderMapper.update(upd);
        log.info("确认送达并完成结算，orderId={}, runnerId={}, income={}", orderId, runnerId, order.getRunnerIncome());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId, Long userId) {
        Orders order = orderMapper.getByIdAndUserId(orderId, userId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != Orders.DELIVERED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders upd = Orders.builder().id(order.getId())
                .status(Orders.COMPLETED)
                .finishTime(LocalDateTime.now())
                .build();
        orderMapper.update(upd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long operatorId, Integer cancelBy, OrdersCancelDTO dto) {
        Orders order = orderMapper.getById(dto.getId());
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //归属校验
        if (cancelBy.equals(RunnerConstant.CANCEL_BY_USER) && !order.getUserId().equals(operatorId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (cancelBy.equals(RunnerConstant.CANCEL_BY_RUNNER)
                && (order.getRunnerId() == null || !order.getRunnerId().equals(operatorId))) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        boolean userCancel = cancelBy.equals(RunnerConstant.CANCEL_BY_USER);
        //用户：待支付/待接单可取消；跑腿员：进行中可取消（订单作废并退款）
        boolean cancellable = userCancel
                ? (order.getStatus() == Orders.PENDING_PAYMENT || order.getStatus() == Orders.TO_BE_TAKEN)
                : order.getStatus() == Orders.IN_PROGRESS;
        if (!cancellable) {
            throw new OrderBusinessException(MessageConstant.ORDER_CANCEL_NOT_ALLOWED);
        }

        //已支付订单退款（钱包余额原路退回）
        if (order.getPayStatus() != null && order.getPayStatus() == Orders.PAID) {
            refund(order);
        }
        Orders upd = Orders.builder().id(order.getId())
                .status(Orders.CANCELLED)
                .cancelReason(dto.getCancelReason())
                .cancelBy(cancelBy)
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(upd);
        log.info("[操作日志]订单取消，orderId={}, 操作方={}({}), 原因={}",
                order.getId(), cancelBy == RunnerConstant.CANCEL_BY_USER ? "用户" : "跑腿员", operatorId, dto.getCancelReason());
    }

    @Override
    public void appeal(Long orderId, Long userId) {
        Orders order = orderMapper.getByIdAndUserId(orderId, userId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Orders upd = Orders.builder().id(order.getId())
                .isAppealed(1)
                .build();
        orderMapper.update(upd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processTimeoutOrders() {
        //1. 未支付超时：直接取消
        List<Orders> unpaid = orderMapper.findUnpaidTimeoutOrders();
        for (Orders order : unpaid) {
            Orders upd = Orders.builder().id(order.getId())
                    .status(Orders.CANCELLED)
                    .cancelBy(RunnerConstant.CANCEL_BY_PLATFORM)
                    .cancelReason("支付超时，系统自动取消")
                    .cancelTime(LocalDateTime.now())
                    .build();
            orderMapper.update(upd);
        }
        //2. 已支付但超时无人接单：标记超时并退款
        List<Orders> unclaimed = orderMapper.findUnclaimedTimeoutOrders(LocalDateTime.now());
        for (Orders order : unclaimed) {
            refund(order);
            Orders upd = Orders.builder().id(order.getId())
                    .status(Orders.TIMEOUT)
                    .cancelBy(RunnerConstant.CANCEL_BY_PLATFORM)
                    .cancelReason("超时无人接单，系统自动退款")
                    .cancelTime(LocalDateTime.now())
                    .build();
            orderMapper.update(upd);
            log.info("[超时订单]无人接单退款，orderId={}, userId={}", order.getId(), order.getUserId());
        }
        //3. 超时未送达：取消订单、退款用户、扣除跑腿员违约金
        List<Orders> overdue = orderMapper.findOverdueDeliveryOrders(LocalDateTime.now());
        for (Orders order : overdue) {
            handleOverdueDelivery(order);
        }
        if (!unpaid.isEmpty() || !unclaimed.isEmpty() || !overdue.isEmpty()) {
            log.info("[超时订单]本轮处理完成：未支付取消 {} 单，无人接单退款 {} 单，超时未送达 {} 单",
                    unpaid.size(), unclaimed.size(), overdue.size());
        }
    }

    /**
     * 超时未送达处理：退款用户，扣除跑腿员违约金（等于其实得金额，余额不足则尽力扣除）
     */
    private void handleOverdueDelivery(Orders order) {
        refund(order);
        if (order.getRunnerId() != null && order.getRunnerIncome() != null) {
            Runner runner = runnerMapper.getById(order.getRunnerId());
            if (runner != null) {
                try {
                    walletService.changeBalance(runner.getUserId(), order.getRunnerIncome().negate(),
                            com.campus.runner.constant.WalletConstant.TYPE_PENALTY, order.getId(),
                            "超时未送达违约金-" + order.getNumber());
                } catch (Exception e) {
                    log.warn("[超时订单]跑腿员违约金扣除失败（余额不足），orderId={}, runnerId={}",
                            order.getId(), order.getRunnerId());
                }
            }
        }
        Orders upd = Orders.builder().id(order.getId())
                .status(Orders.CANCELLED)
                .cancelBy(RunnerConstant.CANCEL_BY_PLATFORM)
                .cancelReason("超时未送达，系统自动取消并退款")
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(upd);
        log.info("[超时订单]超时未送达处理，orderId={}, runnerId={}, 违约金={}",
                order.getId(), order.getRunnerId(), order.getRunnerIncome());
    }

    /**
     * 退款：钱包支付原路退回并生成退款流水，微信支付为模拟退款
     */
    private void refund(Orders order) {
        if (order.getPayMethod() != null && order.getPayMethod() == 2) {
            walletService.changeBalance(order.getUserId(), order.getRewardAmount(),
                    com.campus.runner.constant.WalletConstant.TYPE_REFUND, order.getId(),
                    "订单退款-" + order.getNumber());
        }
        Orders upd = Orders.builder().id(order.getId())
                .payStatus(Orders.REFUND)
                .build();
        orderMapper.update(upd);
    }

    private void checkGrabPermission(Long runnerId, Long orderId) {
        Runner runner = runnerMapper.getById(runnerId);
        if (runner == null) {
            throw new GrabFailedException(MessageConstant.RUNNER_NOT_FOUND);
        }
        if (runner.getAuditStatus() == null || runner.getAuditStatus() != RunnerConstant.AUDIT_PASSED) {
            throw new GrabFailedException(MessageConstant.RUNNER_NOT_CERTIFIED);
        }
        if (runner.getStatus() == null || runner.getStatus() != 1) {
            throw new GrabFailedException(MessageConstant.ACCOUNT_LOCKED);
        }
        int todayCount = orderMapper.countRunnerTodayOrders(runnerId);
        if (runner.getDailyOrderLimit() != null && todayCount >= runner.getDailyOrderLimit()) {
            throw new GrabFailedException(MessageConstant.DAILY_LIMIT_REACHED);
        }
    }

    private Orders getOwnedOrder(Long orderId, Long runnerId) {
        Orders order = orderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (order.getRunnerId() == null || !order.getRunnerId().equals(runnerId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return order;
    }

    private String generateOrderNumber() {
        return "CR" + System.currentTimeMillis() + (int) ((Math.random() * 9 + 1) * 1000);
    }

    private String maskName(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
