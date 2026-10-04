package com.campus.runner.controller.runner;

import com.campus.runner.dto.UserLoginDTO;
import com.campus.runner.result.Result;
import com.campus.runner.service.RunnerService;
import com.campus.runner.vo.UserLoginVO;
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
 * 技能者端-登录接口
 */
@RestController
@Slf4j
@Api(tags = "技能者端-登录接口")
@RequestMapping("/skiller/auth")
public class RunnerAuthController {

    @Autowired
    private RunnerService runnerService;

    @PostMapping("/login")
    @ApiOperation("技能者登录（token中携带技能者身份）")
    public Result<UserLoginVO> login(@RequestBody @Valid UserLoginDTO userLoginDTO) {
        log.info("技能者登录：{}", userLoginDTO.getCode());
        return Result.success(runnerService.login(userLoginDTO.getCode()));
    }
}
