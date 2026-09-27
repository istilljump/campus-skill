package com.campus.runner.controller.user;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.ReviewSubmitDTO;
import com.campus.runner.result.Result;
import com.campus.runner.service.ReviewService;
import com.campus.runner.vo.OrderReviewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 用户端-订单评价接口
 */
@RestController
@Slf4j
@Api(tags = "用户端-订单评价接口")
@RequestMapping("/user/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/submit")
    @ApiOperation("提交评价")
    public Result<String> submit(@RequestBody @Valid ReviewSubmitDTO reviewSubmitDTO) {
        reviewService.submit(BaseContext.getCurrentId(), reviewSubmitDTO);
        return Result.success("评价成功");
    }

    @GetMapping("/order/{orderId}")
    @ApiOperation("查看订单评价")
    public Result<OrderReviewVO> getByOrderId(@PathVariable Long orderId) {
        return Result.success(reviewService.getByOrderId(orderId));
    }
}
