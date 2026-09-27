package com.campus.runner.controller.runner;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.RunnerAuditApplyDTO;
import com.campus.runner.result.Result;
import com.campus.runner.service.RunnerService;
import com.campus.runner.vo.RunnerCenterVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 跑腿员端-个人信息与认证接口
 * 注意：本控制器位于 /runner/** 下，需已认证跑腿员 token 访问；
 * 认证申请入口对未认证用户开放，由用户端接口提供（/user/runner/apply）
 */
@RestController
@Slf4j
@Api(tags = "跑腿员端-个人信息接口")
@RequestMapping("/runner/info")
public class RunnerController {

    @Autowired
    private RunnerService runnerService;

    @GetMapping("/center")
    @ApiOperation("个人中心（今日接单/收入/等级/评分/余额）")
    public Result<RunnerCenterVO> center() {
        return Result.success(runnerService.getCenter(BaseContext.getCurrentId()));
    }
}
