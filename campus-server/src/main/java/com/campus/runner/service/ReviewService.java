package com.campus.runner.service;

import com.campus.runner.dto.ReviewSubmitDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.vo.OrderReviewVO;

public interface ReviewService {

    /**
     * 用户端-提交评价（订单完成后）
     */
    void submit(Long userId, ReviewSubmitDTO reviewSubmitDTO);

    /**
     * 技能者收到的评价分页
     */
    PageResult<OrderReviewVO> pageByRunner(Long runnerId, Integer page, Integer pageSize);

    /**
     * 按订单查询评价
     */
    OrderReviewVO getByOrderId(Long orderId);
}
