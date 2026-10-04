package com.campus.runner.controller.runner;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.RunnerAuditApplyDTO;
import com.campus.runner.result.Result;
import com.campus.runner.service.RunnerService;
import com.campus.runner.vo.RunnerCenterVO;
import com.campus.runner.vo.TrendPointVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 技能者端-个人信息与认证接口
 * 注意：本控制器位于 /runner/** 下，需已认证技能者 token 访问；
 * 认证申请入口对未认证用户开放，由用户端接口提供（/user/runner/apply）
 */
@RestController
@Slf4j
@Api(tags = "技能者端-个人信息接口")
@RequestMapping("/skiller/info")
public class RunnerController {

    @Autowired
    private RunnerService runnerService;

    @GetMapping("/center")
    @ApiOperation("个人中心（今日接单/收入/等级/评分/余额）")
    public Result<RunnerCenterVO> center() {
        //BaseContext 中是技能者ID，getCenter 需要关联用户ID
        return Result.success(runnerService.getCenter(runnerService.resolveUserId(BaseContext.getCurrentId())));
    }

    @GetMapping("/trend")
    @ApiOperation("近 N 日完成单量与收入趋势（默认7天）")
    public Result<List<TrendPointVO>> trend(@RequestParam(defaultValue = "7") Integer days) {
        Long userId = runnerService.resolveUserId(BaseContext.getCurrentId());
        return Result.success(runnerService.dailyTrend(userId, days));
    }
}
