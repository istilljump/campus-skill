package com.campus.runner.service.impl;

import com.campus.runner.constant.MessageConstant;
import com.campus.runner.dto.BookingApplyDTO;
import com.campus.runner.entity.Booking;
import com.campus.runner.entity.ErrandType;
import com.campus.runner.entity.Orders;
import com.campus.runner.entity.Runner;
import com.campus.runner.entity.ServiceItem;
import com.campus.runner.exception.BusinessException;
import com.campus.runner.mapper.BookingMapper;
import com.campus.runner.mapper.ErrandTypeMapper;
import com.campus.runner.mapper.OrderMapper;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.mapper.ServiceItemMapper;
import com.campus.runner.mapper.UserMapper;
import com.campus.runner.result.PageResult;
import com.campus.runner.service.BookingService;
import com.campus.runner.service.MessageService;
import com.campus.runner.vo.BookingVO;
import com.campus.runner.vo.OrderSubmitVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    //类目缺失时的默认平台服务费率
    private static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.10");

    //订单号随机位
    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private ServiceItemMapper serviceItemMapper;

    @Autowired
    private ErrandTypeMapper errandTypeMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MessageService messageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long userId, BookingApplyDTO dto) {
        ServiceItem service = serviceItemMapper.getById(dto.getServiceItemId());
        if (service == null || service.getStatus() == null || service.getStatus() != ServiceItem.ON_SHELF) {
            throw new BusinessException("服务不存在或已下架");
        }
        Booking booking = Booking.builder()
                .serviceItemId(service.getId())
                .userId(userId)
                .skillerId(service.getSkillerId())
                .expectTime(parseExpectTime(dto.getExpectTime()))
                .remark(dto.getRemark())
                .status(Booking.PENDING_CONFIRM)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        bookingMapper.insert(booking);
        //站内消息：通知技能者有新的预约
        Runner skiller = runnerMapper.getById(service.getSkillerId());
        if (skiller != null) {
            messageService.notify(skiller.getUserId(), "收到新的服务预约",
                    "您挂牌的服务「" + service.getTitle() + "」收到新的预约，请及时确认",
                    null);
        }
        log.info("服务预约提交成功，bookingId={}, userId={}, serviceItemId={}", booking.getId(), userId, service.getId());
    }

    /**
     * 期望时间宽容解析：支持 2026-10-06 14:00 与 2026-10-06 14:00:00 两种格式，填错给出友好提示
     */
    private LocalDateTime parseExpectTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        try {
            return LocalDateTime.parse(t, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(t, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(t);
        } catch (Exception ignored) {
        }
        throw new BusinessException("期望时间格式不正确，请按 2026-10-06 14:00 填写");
    }

    @Override
    public PageResult<BookingVO> userPage(Long userId, Integer page, Integer pageSize) {
        PageHelper.startPage(page, pageSize);
        Page<BookingVO> voPage = (Page<BookingVO>) bookingMapper.listByUser(userId);
        return new PageResult<>(voPage.getTotal(), voPage.getResult(), voPage.getPageSize(), voPage.getPageNum());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long id) {
        Booking booking = getUserBooking(userId, id);
        if (booking.getStatus() == null || booking.getStatus() != Booking.PENDING_CONFIRM) {
            throw new BusinessException("仅待确认的预约可取消");
        }
        bookingMapper.updateStatus(id, Booking.CANCELLED);
        log.info("预约已取消，bookingId={}, userId={}", id, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO pay(Long userId, Long id) {
        Booking booking = getUserBooking(userId, id);
        if (booking.getStatus() == null || booking.getStatus() != Booking.CONFIRMED) {
            throw new BusinessException("仅技能者已确认的预约可支付");
        }
        if (booking.getOrderId() != null) {
            throw new BusinessException("该预约已生成订单，请勿重复支付");
        }
        ServiceItem service = serviceItemMapper.getById(booking.getServiceItemId());
        if (service == null) {
            throw new BusinessException("服务不存在或已下架");
        }

        //服务费按类目费率计算，类目缺失时取默认费率
        ErrandType category = errandTypeMapper.getById(service.getCategoryId());
        BigDecimal feeRate = category != null && category.getFeeRate() != null
                ? category.getFeeRate() : DEFAULT_FEE_RATE;
        BigDecimal platformFee = service.getPrice().multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal skillerIncome = service.getPrice().subtract(platformFee);

        Runner skiller = runnerMapper.getById(service.getSkillerId());
        LocalDateTime now = LocalDateTime.now();
        Orders order = Orders.builder()
                .number("CR" + System.currentTimeMillis()
                        + String.format("%06d", SECURE_RANDOM.nextInt(1_000_000)))
                .userId(userId)
                .runnerId(service.getSkillerId())
                .typeId(service.getCategoryId())
                .mode(Orders.MODE_BOOKING)
                .serviceItemId(service.getId())
                .reworkCount(0)
                .title(service.getTitle())
                .description("服务预约：" + service.getTitle())
                .deliveryAddress(service.getServiceMode() != null && service.getServiceMode() == ServiceItem.MODE_OFFLINE
                        ? "线下服务" : "线上交付")
                .campus(skiller != null ? skiller.getCampus() : null)
                .rewardAmount(service.getPrice())
                .platformFee(platformFee)
                .runnerIncome(skillerIncome)
                .status(Orders.PENDING_PAYMENT)
                .payMethod(2)
                .payStatus(Orders.UN_PAID)
                .orderTime(now)
                //交付截止按服务承诺的交付天数计算，避免长周期技能订单被超时任务误杀
                .timeoutTime(now.plusDays(service.getDeliveryDays() == null ? 2 : service.getDeliveryDays()))
                .createTime(now)
                .updateTime(now)
                .build();
        orderMapper.insert(order);
        //预约单回填订单id（状态保持已确认，订单验收完成时置为已完成）
        bookingMapper.updateOrderId(booking.getId(), order.getId());

        return OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .rewardAmount(service.getPrice())
                .platformFee(platformFee)
                .payAmount(service.getPrice())
                .build();
    }

    @Override
    public PageResult<BookingVO> skillerPage(Long skillerId, Integer status, Integer page, Integer pageSize) {
        PageHelper.startPage(page, pageSize);
        Page<BookingVO> voPage = (Page<BookingVO>) bookingMapper.listBySkiller(skillerId, status);
        return new PageResult<>(voPage.getTotal(), voPage.getResult(), voPage.getPageSize(), voPage.getPageNum());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long skillerId, Long id) {
        Booking booking = getSkillerBooking(skillerId, id);
        if (booking.getStatus() == null || booking.getStatus() != Booking.PENDING_CONFIRM) {
            throw new BusinessException("仅待确认的预约可确认");
        }
        bookingMapper.updateStatus(id, Booking.CONFIRMED);
        ServiceItem service = serviceItemMapper.getById(booking.getServiceItemId());
        messageService.notify(booking.getUserId(), "预约已确认",
                "您的预约「" + (service != null ? service.getTitle() : "") + "」已被技能者确认，请及时支付",
                null);
        log.info("预约已确认，bookingId={}, skillerId={}", id, skillerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long skillerId, Long id) {
        Booking booking = getSkillerBooking(skillerId, id);
        if (booking.getStatus() == null || booking.getStatus() != Booking.PENDING_CONFIRM) {
            throw new BusinessException("仅待确认的预约可拒绝");
        }
        bookingMapper.updateStatus(id, Booking.REJECTED);
        ServiceItem service = serviceItemMapper.getById(booking.getServiceItemId());
        messageService.notify(booking.getUserId(), "预约已被拒绝",
                "很抱歉，您的预约「" + (service != null ? service.getTitle() : "") + "」被技能者拒绝",
                null);
        log.info("预约已拒绝，bookingId={}, skillerId={}", id, skillerId);
    }

    private Booking getUserBooking(Long userId, Long id) {
        Booking booking = bookingMapper.getById(id);
        if (booking == null || !booking.getUserId().equals(userId)) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return booking;
    }

    private Booking getSkillerBooking(Long skillerId, Long id) {
        Booking booking = bookingMapper.getById(id);
        if (booking == null || !booking.getSkillerId().equals(skillerId)) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return booking;
    }
}
