package com.campus.runner.service.impl;

import com.campus.runner.constant.MessageConstant;
import com.campus.runner.constant.RedisConstant;
import com.campus.runner.constant.RunnerConstant;
import com.campus.runner.dto.DisputeApplyDTO;
import com.campus.runner.dto.DisputeVerdictDTO;
import com.campus.runner.dto.OrdersBoostDTO;
import com.campus.runner.dto.OrdersCancelDTO;
import com.campus.runner.dto.OrdersDeliverDTO;
import com.campus.runner.dto.OrdersGrabDTO;
import com.campus.runner.dto.OrdersPageQueryDTO;
import com.campus.runner.dto.OrdersSubmitDTO;
import com.campus.runner.dto.ReworkDTO;
import com.campus.runner.entity.AddressBook;
import com.campus.runner.entity.Booking;
import com.campus.runner.entity.Dispute;
import com.campus.runner.entity.ErrandType;
import com.campus.runner.entity.Orders;
import com.campus.runner.entity.Runner;
import com.campus.runner.entity.ServiceItem;
import com.campus.runner.entity.User;
import com.campus.runner.exception.BusinessException;
import com.campus.runner.exception.GrabFailedException;
import com.campus.runner.exception.OrderBusinessException;
import com.campus.runner.mapper.AddressBookMapper;
import com.campus.runner.mapper.BookingMapper;
import com.campus.runner.mapper.DisputeMapper;
import com.campus.runner.mapper.ErrandTypeMapper;
import com.campus.runner.mapper.OrderMapper;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.mapper.ServiceItemMapper;
import com.campus.runner.mapper.UserMapper;
import com.campus.runner.result.PageResult;
import com.campus.runner.service.CreditService;
import com.campus.runner.service.MessageService;
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

    //订单号随机位
    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

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
    private BookingMapper bookingMapper;

    @Autowired
    private DisputeMapper disputeMapper;

    @Autowired
    private ServiceItemMapper serviceItemMapper;

    @Autowired
    private WalletService walletService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private com.campus.runner.service.RunnerService runnerService;

    @Autowired
    private CreditService creditService;

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
            throw new BusinessException("送达地址不能为空：请选择地址簿联系人或直接填写交付地址");
        }

        //平台服务费按类型费率从悬赏中扣除，技能者实得 = 悬赏 - 服务费
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
                .mode(Orders.MODE_REWARD)
                .reworkCount(0)
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
                    com.campus.runner.constant.WalletConstant.TYPE_EXPENSE, order.getId(), "技能订单支付-" + order.getNumber());
        }
        Orders upd = Orders.builder().id(order.getId())
                .payStatus(Orders.PAID)
                .payTime(LocalDateTime.now())
                .build();
        if (Orders.MODE_BOOKING.equals(order.getMode())) {
            //预约模式：双方已确认，支付后直接进入进行中，无需再进大厅抢单
            upd.setStatus(Orders.IN_PROGRESS);
            upd.setPickupTime(LocalDateTime.now());
        } else {
            upd.setStatus(Orders.TO_BE_TAKEN);
        }
        orderMapper.update(upd);
        if (Orders.MODE_BOOKING.equals(order.getMode()) && order.getRunnerId() != null) {
            Runner skiller = runnerMapper.getById(order.getRunnerId());
            if (skiller != null) {
                messageService.notify(skiller.getUserId(), "预约订单已支付",
                        "订单「" + order.getTitle() + "」已完成支付，请按预约时间开始服务并及时交付", order.getId());
            }
        }
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
                .mode(order.getMode())
                .serviceItemId(order.getServiceItemId())
                .pickupAddress(order.getPickupAddress())
                .deliveryAddress(order.getDeliveryAddress())
                .deliverableUrl(order.getDeliverableUrl())
                .deliverableNote(order.getDeliverableNote())
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
                .deliverTime(order.getDeliverTime())
                .reworkCount(order.getReworkCount())
                .autoAcceptTime(order.getAutoAcceptTime())
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
        //限流：同一技能者 10 秒内最多 5 次抢单请求，防止刷接口
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
            //站内消息：通知发单用户
            Orders grabbed = orderMapper.getById(orderId);
            if (grabbed != null) {
                //用户未定期限的技能订单：接单后交付窗口从2小时(接单等待)放宽为24小时，避免被超时任务误杀
                if (grabbed.getExpectedTime() == null) {
                    orderMapper.update(Orders.builder().id(orderId)
                            .timeoutTime(LocalDateTime.now().plusHours(24))
                            .build());
                }
                Runner grabRunner = runnerMapper.getById(runnerId);
                messageService.notify(grabbed.getUserId(), "订单已被接单",
                        "您的订单「" + grabbed.getTitle() + "」已被"
                                + (grabRunner != null ? grabRunner.getName() : "技能者") + "接单，请留意服务进度",
                        grabbed.getId());
            }
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void boost(Long userId, OrdersBoostDTO dto) {
        Orders order = orderMapper.getByIdAndUserId(dto.getId(), userId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != Orders.TO_BE_TAKEN || order.getPayStatus() == null || order.getPayStatus() != Orders.PAID) {
            throw new OrderBusinessException("仅已支付且待接单的订单可追加悬赏");
        }
        BigDecimal amount = dto.getAmount();
        //钱包支付的订单从余额扣款（changeBalance 保证余额不透支），微信支付订单为模拟支付直接成功
        if (order.getPayMethod() != null && order.getPayMethod() == 2) {
            walletService.changeBalance(userId, amount.negate(),
                    com.campus.runner.constant.WalletConstant.TYPE_EXPENSE, order.getId(),
                    "追加悬赏-" + order.getNumber());
        }
        //按类型费率重算服务费与技能者实得
        ErrandType type = errandTypeMapper.getById(order.getTypeId());
        BigDecimal feeRate = type != null && type.getFeeRate() != null ? type.getFeeRate() : new BigDecimal("0.10");
        BigDecimal newReward = order.getRewardAmount().add(amount);
        BigDecimal newFee = newReward.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newIncome = newReward.subtract(newFee);
        Orders upd = Orders.builder()
                .id(order.getId())
                .rewardAmount(newReward)
                .platformFee(newFee)
                .runnerIncome(newIncome)
                .build();
        //追加悬赏顺延超时时间，给订单更多曝光接单机会
        if (order.getTimeoutTime() != null) {
            upd.setTimeoutTime(order.getTimeoutTime().plusMinutes(15));
        }
        orderMapper.update(upd);
        messageService.notify(userId, "追加悬赏成功",
                "订单「" + order.getTitle() + "」悬赏已追加至 ¥" + newReward + "，将继续等待技能者接单", order.getId());
        log.info("追加悬赏成功，orderId={}, userId={}, amount={}, newReward={}", order.getId(), userId, amount, newReward);
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
        messageService.notify(order.getUserId(), "订单已取件",
                "订单「" + order.getTitle() + "」技能者已确认取件，正在配送中", order.getId());
        log.info("确认取件，orderId={}, runnerId={}", orderId, runnerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliver(Long orderId, Long runnerId, OrdersDeliverDTO dto) {
        Orders order = getOwnedOrder(orderId, runnerId);
        if (order.getStatus() != Orders.IN_PROGRESS) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //交付物地址必填
        if (dto == null || dto.getDeliverableUrl() == null || dto.getDeliverableUrl().isBlank()) {
            throw new BusinessException("请填写交付物地址");
        }
        LocalDateTime now = LocalDateTime.now();
        Orders upd = Orders.builder().id(order.getId())
                .status(Orders.DELIVERED)
                .deliverableUrl(dto.getDeliverableUrl())
                .deliverableNote(dto.getDeliverableNote())
                .deliverTime(now)
                //48小时未验收自动确认
                .autoAcceptTime(now.plusHours(48))
                .build();
        orderMapper.update(upd);
        //站内消息：通知用户及时验收
        messageService.notify(order.getUserId(), "技能服务已交付",
                "订单「" + order.getTitle() + "」已交付，请及时验收（48小时未验收将自动确认）", order.getId());
        log.info("提交交付物，orderId={}, runnerId={}, deliverableUrl={}", orderId, runnerId, dto.getDeliverableUrl());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long orderId, Long userId) {
        Orders order = orderMapper.getByIdAndUserId(orderId, userId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != Orders.DELIVERED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        settleAndComplete(order);
        //站内消息：通知技能者报酬到账
        notifySkilledIncome(order, "订单已验收",
                "订单「" + order.getTitle() + "」已验收，报酬 ¥" + order.getRunnerIncome() + " 已到账");
        log.info("订单验收完成，orderId={}, userId={}, income={}", orderId, userId, order.getRunnerIncome());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId, Long userId) {
        //旧路由兼容：确认完成等价于验收
        accept(orderId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rework(Long orderId, Long userId, ReworkDTO dto) {
        Orders order = orderMapper.getByIdAndUserId(orderId, userId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != Orders.DELIVERED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        int reworkCount = order.getReworkCount() == null ? 0 : order.getReworkCount();
        if (reworkCount >= 2) {
            throw new BusinessException("返修次数已用完，可发起仲裁");
        }
        Orders upd = Orders.builder().id(order.getId())
                .status(Orders.REWORK)
                .reworkCount(reworkCount + 1)
                .build();
        orderMapper.update(upd);
        //返修扣减技能者信用分
        creditService.addCredit(order.getRunnerId(), -3, "返修-3", order.getId());
        //站内消息：通知技能者返修原因
        notifySkilledIncome(order, "订单需返修",
                "订单「" + order.getTitle() + "」被用户申请返修" + (dto != null && dto.getReason() != null ? "，原因：" + dto.getReason() : "")
                        + "，请及时响应并重新交付");
        log.info("订单申请返修，orderId={}, userId={}, reworkCount={}", orderId, userId, reworkCount + 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void respondRework(Long orderId, Long runnerId) {
        Orders order = getOwnedOrder(orderId, runnerId);
        if (order.getStatus() != Orders.REWORK) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders upd = Orders.builder().id(order.getId())
                .status(Orders.IN_PROGRESS)
                //清空旧交付物，等待重新交付
                .deliverableUrl("")
                .deliverableNote("")
                .build();
        orderMapper.update(upd);
        //站内消息：通知用户重新交付中
        messageService.notify(order.getUserId(), "技能者已响应返修",
                "订单「" + order.getTitle() + "」技能者已响应返修，正在重新交付", order.getId());
        log.info("响应返修，orderId={}, runnerId={}", orderId, runnerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyDispute(Long orderId, Long userId, DisputeApplyDTO dto) {
        Orders order = orderMapper.getByIdAndUserId(orderId, userId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != Orders.DELIVERED && order.getStatus() != Orders.REWORK) {
            throw new OrderBusinessException("仅已交付或返修中的订单可发起仲裁");
        }
        Dispute dispute = Dispute.builder()
                .orderId(order.getId())
                .raisedBy(userId)
                .reasonType(dto.getReasonType())
                .description(dto.getDescription())
                .evidenceUrls(dto.getEvidenceUrls())
                .status(Dispute.PENDING)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        disputeMapper.insert(dispute);
        Orders upd = Orders.builder().id(order.getId())
                .status(Orders.DISPUTE)
                .build();
        orderMapper.update(upd);
        //站内消息：通知技能者订单进入仲裁
        notifySkilledIncome(order, "订单发起仲裁",
                "订单「" + order.getTitle() + "」已发起仲裁，平台将尽快介入处理，请留意仲裁结果");
        log.info("订单发起仲裁，orderId={}, userId={}, disputeId={}", orderId, userId, dispute.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verdictDispute(Long disputeId, DisputeVerdictDTO dto, Long adminId) {
        Dispute dispute = disputeMapper.getById(disputeId);
        if (dispute == null) {
            throw new BusinessException("仲裁工单不存在");
        }
        if (dispute.getStatus() == null || dispute.getStatus() != Dispute.PENDING) {
            throw new BusinessException("该工单已仲裁，请勿重复处理");
        }
        Orders order = orderMapper.getById(dispute.getOrderId());
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        String verdictText = dto.getVerdict() == null ? "" : dto.getVerdict();
        if (dto.getStatus() != null && dto.getStatus() == Dispute.REFUND_USER) {
            //1退款用户：退款 + 扣技能者报酬（尽力扣除）+ 扣信用分
            refund(order);
            if (order.getRunnerId() != null && order.getRunnerIncome() != null) {
                Runner runner = runnerMapper.getById(order.getRunnerId());
                if (runner != null) {
                    //尽力扣除技能者报酬作为判责违约金：余额不足时扣到0为止
                    walletService.deductBestEffort(runner.getUserId(), order.getRunnerIncome(),
                            com.campus.runner.constant.WalletConstant.TYPE_PENALTY, order.getId(),
                            "仲裁判责违约金-" + order.getNumber());
                    creditService.addCredit(order.getRunnerId(), -10, "仲裁判责-10", order.getId());
                }
            }
            Orders upd = Orders.builder().id(order.getId())
                    .status(Orders.CANCELLED)
                    .cancelBy(RunnerConstant.CANCEL_BY_PLATFORM)
                    .cancelReason("仲裁判定：退款用户")
                    .cancelTime(LocalDateTime.now())
                    .build();
            orderMapper.update(upd);
            disputeMapper.updateStatusVerdict(disputeId, Dispute.REFUND_USER, dto.getVerdict(), adminId);
            messageService.notify(order.getUserId(), "仲裁结果",
                    "订单「" + order.getTitle() + "」仲裁判定：退款用户。已为您退款" + (verdictText.isEmpty() ? "" : "，仲裁意见：" + verdictText),
                    order.getId());
            notifySkilledIncome(order, "仲裁结果",
                    "订单「" + order.getTitle() + "」仲裁判定：退款用户，报酬不予结算并扣除相应信用分" + (verdictText.isEmpty() ? "" : "，仲裁意见：" + verdictText));
            log.info("[仲裁]退款用户判决完成，disputeId={}, orderId={}", disputeId, order.getId());
        } else if (dto.getStatus() != null && dto.getStatus() == Dispute.RELEASE_SKILLER) {
            //2放款技能者：按正常验收完成结算
            settleAndComplete(order);
            disputeMapper.updateStatusVerdict(disputeId, Dispute.RELEASE_SKILLER, dto.getVerdict(), adminId);
            messageService.notify(order.getUserId(), "仲裁结果",
                    "订单「" + order.getTitle() + "」仲裁判定：交付合格，订单已确认完成" + (verdictText.isEmpty() ? "" : "，仲裁意见：" + verdictText),
                    order.getId());
            notifySkilledIncome(order, "仲裁结果",
                    "订单「" + order.getTitle() + "」仲裁判定：交付合格，报酬 ¥" + order.getRunnerIncome() + " 已到账" + (verdictText.isEmpty() ? "" : "，仲裁意见：" + verdictText));
            log.info("[仲裁]放款技能者判决完成，disputeId={}, orderId={}", disputeId, order.getId());
        } else if (dto.getStatus() != null && dto.getStatus() == Dispute.REJECTED) {
            //3驳回：订单回到待验收，重新起算自动验收时间
            Orders upd = Orders.builder().id(order.getId())
                    .status(Orders.DELIVERED)
                    .autoAcceptTime(LocalDateTime.now().plusHours(48))
                    .build();
            orderMapper.update(upd);
            disputeMapper.updateStatusVerdict(disputeId, Dispute.REJECTED, dto.getVerdict(), adminId);
            messageService.notify(order.getUserId(), "仲裁结果",
                    "订单「" + order.getTitle() + "」仲裁已驳回，订单恢复待验收" + (verdictText.isEmpty() ? "" : "，仲裁意见：" + verdictText),
                    order.getId());
            notifySkilledIncome(order, "仲裁结果",
                    "订单「" + order.getTitle() + "」仲裁已驳回，订单恢复待验收" + (verdictText.isEmpty() ? "" : "，仲裁意见：" + verdictText));
            log.info("[仲裁]驳回判决完成，disputeId={}, orderId={}", disputeId, order.getId());
        } else {
            throw new BusinessException("仲裁结果不合法");
        }
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
        //用户：待支付/待接单可取消；技能者：进行中可取消（订单作废并退款）
        boolean cancellable = userCancel
                ? (order.getStatus() == Orders.PENDING_PAYMENT || order.getStatus() == Orders.TO_BE_TAKEN)
                : order.getStatus() == Orders.IN_PROGRESS;
        if (!cancellable) {
            throw new OrderBusinessException(MessageConstant.ORDER_CANCEL_NOT_ALLOWED);
        }

        //已支付订单退款（钱包余额原路退回），用户单方面取消扣2分信用分
        if (order.getPayStatus() != null && order.getPayStatus() == Orders.PAID) {
            refund(order);
            if (cancelBy.equals(RunnerConstant.CANCEL_BY_USER) && order.getStatus() != Orders.PENDING_PAYMENT) {
                userMapper.adjustCreditScore(order.getUserId(), -2);
                log.info("[操作日志]用户取消已支付订单扣信用分，orderId={}, userId={}, -2分", order.getId(), order.getUserId());
            }
        }
        Orders upd = Orders.builder().id(order.getId())
                .status(Orders.CANCELLED)
                .cancelReason(dto.getCancelReason())
                .cancelBy(cancelBy)
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(upd);
        //站内消息：通知对方订单已取消
        if (userCancel && order.getRunnerId() != null) {
            Runner cancelledRunner = runnerMapper.getById(order.getRunnerId());
            if (cancelledRunner != null) {
                messageService.notify(cancelledRunner.getUserId(), "订单已取消",
                        "订单「" + order.getTitle() + "」已被用户取消" + (order.getPayStatus() == Orders.PAID ? "，报酬不会结算" : ""),
                        order.getId());
            }
        } else if (!userCancel) {
            messageService.notify(order.getUserId(), "订单已取消",
                    "您的订单「" + order.getTitle() + "」已被技能者取消，已支付金额将自动退回", order.getId());
        }
        log.info("[操作日志]订单取消，orderId={}, 操作方={}({}), 原因={}",
                order.getId(), cancelBy == RunnerConstant.CANCEL_BY_USER ? "用户" : "技能者", operatorId, dto.getCancelReason());
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
        //3. 超时未交付（含返修中超时）：取消订单、退款用户、扣除技能者违约金与信用分
        List<Orders> overdue = orderMapper.findOverdueDeliveryOrders(LocalDateTime.now());
        for (Orders order : overdue) {
            handleOverdueDelivery(order);
        }
        //4. 48小时未验收：系统自动验收并结算
        List<Orders> autoAccept = orderMapper.findAutoAcceptOrders(LocalDateTime.now());
        for (Orders order : autoAccept) {
            settleAndComplete(order);
            notifySkilledIncome(order, "系统自动验收",
                    "订单「" + order.getTitle() + "」已超过48小时未验收，系统已自动验收，报酬 ¥" + order.getRunnerIncome() + " 已到账");
            log.info("[超时订单]系统自动验收，orderId={}, userId={}", order.getId(), order.getUserId());
        }
        if (!unpaid.isEmpty() || !unclaimed.isEmpty() || !overdue.isEmpty() || !autoAccept.isEmpty()) {
            log.info("[超时订单]本轮处理完成：未支付取消 {} 单，无人接单退款 {} 单，超时未交付 {} 单，自动验收 {} 单",
                    unpaid.size(), unclaimed.size(), overdue.size(), autoAccept.size());
        }
    }

    /**
     * 结算并完成订单（用户验收 / 系统自动验收 / 仲裁放款技能者共用）：
     * 技能者到账 = 悬赏 - 平台服务费，累计完成数、更新等级、信用分+1、服务销量与预约单联动
     */
    private void settleAndComplete(Orders order) {
        if (order.getRunnerId() == null) {
            throw new OrderBusinessException(MessageConstant.RUNNER_NOT_FOUND);
        }
        Runner runner = runnerMapper.getById(order.getRunnerId());
        if (runner == null) {
            throw new OrderBusinessException(MessageConstant.RUNNER_NOT_FOUND);
        }
        walletService.changeBalance(runner.getUserId(), order.getRunnerIncome(),
                com.campus.runner.constant.WalletConstant.TYPE_INCOME, order.getId(),
                "技能服务收入-" + order.getNumber());
        //累计完成订单数并自动更新技能等级
        runnerMapper.incrementCompletedOrders(order.getRunnerId());
        runnerService.updateLevel(order.getRunnerId());
        //信用分+1
        creditService.addCredit(order.getRunnerId(), 1, "交付完成+1", order.getId());
        //预约模式订单：累计服务销量
        if (order.getMode() != null && order.getMode() == Orders.MODE_BOOKING && order.getServiceItemId() != null) {
            serviceItemMapper.incrementSales(order.getServiceItemId());
        }
        //关联预约单置为已完成
        Booking booking = bookingMapper.getByOrderId(order.getId());
        if (booking != null) {
            bookingMapper.updateStatus(booking.getId(), Booking.COMPLETED);
        }
        Orders upd = Orders.builder().id(order.getId())
                .status(Orders.COMPLETED)
                .finishTime(LocalDateTime.now())
                .build();
        orderMapper.update(upd);
    }

    /**
     * 站内消息：通知技能者关联用户（经技能者记录解析 userId）
     */
    private void notifySkilledIncome(Orders order, String title, String content) {
        if (order.getRunnerId() == null) {
            return;
        }
        Runner runner = runnerMapper.getById(order.getRunnerId());
        if (runner != null) {
            messageService.notify(runner.getUserId(), title, content, order.getId());
        }
    }

    /**
     * 超时未交付处理：退款用户，扣除技能者违约金（等于其实得金额，余额不足则尽力扣除）并扣信用分
     */
    private void handleOverdueDelivery(Orders order) {
        refund(order);
        if (order.getRunnerId() != null && order.getRunnerIncome() != null) {
            Runner runner = runnerMapper.getById(order.getRunnerId());
            if (runner != null) {
                //尽力扣除违约金：余额不足时扣到0为止，不影响外层事务
                walletService.deductBestEffort(runner.getUserId(), order.getRunnerIncome(),
                        com.campus.runner.constant.WalletConstant.TYPE_PENALTY, order.getId(),
                        "超时未交付违约金-" + order.getNumber());
                //超时未交付扣减信用分
                creditService.addCredit(order.getRunnerId(), -5, "超时未交付-5", order.getId());
            }
        }
        Orders upd = Orders.builder().id(order.getId())
                .status(Orders.CANCELLED)
                .cancelBy(RunnerConstant.CANCEL_BY_PLATFORM)
                .cancelReason("超时未交付，系统自动取消并退款")
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(upd);
        log.info("[超时订单]超时未交付处理，orderId={}, runnerId={}, 违约金={}",
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
        //SecureRandom 避免可预测订单号，毫秒+6位随机降低撞号概率
        return "CR" + System.currentTimeMillis()
                + String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String maskName(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
