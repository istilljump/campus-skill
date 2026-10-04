package com.campus.runner.service;

import com.campus.runner.dto.BookingApplyDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.vo.BookingVO;
import com.campus.runner.vo.OrderSubmitVO;

public interface BookingService {

    /**
     * 用户端-预约服务（生成待确认预约单并通知技能者）
     */
    void apply(Long userId, BookingApplyDTO bookingApplyDTO);

    /**
     * 用户端-我的预约分页
     */
    PageResult<BookingVO> userPage(Long userId, Integer page, Integer pageSize);

    /**
     * 用户端-取消预约（仅待确认可取消）
     */
    void cancel(Long userId, Long id);

    /**
     * 用户端-预约支付（技能者已确认后下单，生成待支付订单）
     */
    OrderSubmitVO pay(Long userId, Long id);

    /**
     * 技能者端-收到的预约分页（可按状态过滤）
     */
    PageResult<BookingVO> skillerPage(Long skillerId, Integer status, Integer page, Integer pageSize);

    /**
     * 技能者端-确认预约
     */
    void confirm(Long skillerId, Long id);

    /**
     * 技能者端-拒绝预约
     */
    void reject(Long skillerId, Long id);
}
