package com.campus.runner.controller.user;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.RunnerAuditApplyDTO;
import com.campus.runner.result.Result;
import com.campus.runner.service.RunnerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 用户端-成为跑腿员（认证申请入口，未认证用户即可访问）
 */
@RestController
@Slf4j
@Api(tags = "用户端-跑腿员认证申请")
@RequestMapping("/user/runner")
public class RunnerApplyController {

    @Autowired
    private RunnerService runnerService;

    @PostMapping("/apply")
    @ApiOperation("提交跑腿员认证申请")
    public Result<String> apply(@RequestBody @Valid RunnerAuditApplyDTO runnerAuditApplyDTO) {
        runnerService.applyAudit(BaseContext.getCurrentId(), runnerAuditApplyDTO);
        return Result.success("认证申请已提交，请等待审核");
    }
}
