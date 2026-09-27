package com.campus.runner.service.impl;

import com.campus.runner.constant.MessageConstant;
import com.campus.runner.dto.ReviewSubmitDTO;
import com.campus.runner.entity.OrderReview;
import com.campus.runner.entity.Orders;
import com.campus.runner.entity.User;
import com.campus.runner.exception.BusinessException;
import com.campus.runner.mapper.OrderMapper;
import com.campus.runner.mapper.OrderReviewMapper;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.mapper.UserMapper;
import com.campus.runner.result.PageResult;
import com.campus.runner.service.ReviewService;
import com.campus.runner.service.RunnerService;
import com.campus.runner.vo.OrderReviewVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private OrderReviewMapper orderReviewMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Autowired
    private RunnerService runnerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long userId, ReviewSubmitDTO dto) {
        Orders order = orderMapper.getByIdAndUserId(dto.getOrderId(), userId);
        if (order == null) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != Orders.COMPLETED) {
            throw new BusinessException(MessageConstant.REVIEW_NOT_ALLOWED);
        }
        if (order.getRunnerId() == null) {
            throw new BusinessException(MessageConstant.RUNNER_NOT_FOUND);
        }
        if (orderReviewMapper.getByOrderId(order.getId()) != null) {
            throw new BusinessException(MessageConstant.REVIEW_EXISTS);
        }

        OrderReview review = OrderReview.builder()
                .orderId(order.getId())
                .userId(userId)
                .runnerId(order.getRunnerId())
                .score(dto.getScore())
                .content(dto.getContent())
                .tags(dto.getTags())
                .isAnonymous(dto.getIsAnonymous() == null ? 0 : dto.getIsAnonymous())
                .createTime(LocalDateTime.now())
                .build();
        orderReviewMapper.insert(review);
        //重算跑腿员综合评分
        runnerService.updateScore(order.getRunnerId());
    }

    @Override
    public PageResult<OrderReviewVO> pageByRunner(Long runnerId, Integer page, Integer pageSize) {
        PageHelper.startPage(page, pageSize);
        Page<OrderReview> reviewPage = orderReviewMapper.pageByRunnerId(runnerId);
        List<OrderReviewVO> vos = reviewPage.getResult().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(reviewPage.getTotal(), vos, reviewPage.getPageSize(), reviewPage.getPageNum());
    }

    @Override
    public OrderReviewVO getByOrderId(Long orderId) {
        OrderReview review = orderReviewMapper.getByOrderId(orderId);
        return review == null ? null : toVO(review);
    }

    private OrderReviewVO toVO(OrderReview review) {
        OrderReviewVO vo = OrderReviewVO.builder()
                .id(review.getId())
                .orderId(review.getOrderId())
                .score(review.getScore())
                .content(review.getContent())
                .tags(review.getTags())
                .isAnonymous(review.getIsAnonymous())
                .createTime(review.getCreateTime())
                .build();
        User reviewer = userMapper.getById(review.getUserId());
        if (reviewer != null) {
            String name = reviewer.getName();
            //匿名评价脱敏
            if (review.getIsAnonymous() != null && review.getIsAnonymous() == 1 && name != null && !name.isEmpty()) {
                name = name.charAt(0) + "**";
            }
            vo.setUserName(name);
        }
        com.campus.runner.entity.Runner runner = runnerMapper.getById(review.getRunnerId());
        if (runner != null) {
            vo.setRunnerName(runner.getName());
        }
        return vo;
    }
}
