package com.campus.runner.controller.runner;

import com.campus.runner.dto.AccountLoginDTO;
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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${campus.guest-login.enabled:true}")
    private boolean guestLoginEnabled;

    @PostMapping("/login")
    @ApiOperation("技能者登录（体验码降级入口，可用 campus.guest-login.enabled 关闭）")
    public Result<UserLoginVO> login(@RequestBody @Valid UserLoginDTO userLoginDTO) {
        if (!guestLoginEnabled) {
            throw new com.campus.runner.exception.BusinessException("体验登录已关闭，请使用账号密码登录");
        }
        log.info("技能者登录：{}", userLoginDTO.getCode());
        return Result.success(runnerService.login(userLoginDTO.getCode()));
    }

    @PostMapping("/accountLogin")
    @ApiOperation("技能者账号密码登录（与用户端共用账号体系，需已认证）")
    public Result<UserLoginVO> accountLogin(@RequestBody @Valid AccountLoginDTO dto) {
        return Result.success(runnerService.loginByAccount(dto));
    }
}
