package com.campus.runner.controller.user;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.UserLoginDTO;
import com.campus.runner.entity.User;
import com.campus.runner.result.Result;
import com.campus.runner.service.UserService;
import com.campus.runner.vo.UserLoginVO;
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

@RestController
@Slf4j
@Api(tags = "小程序用户相关接口")
@RequestMapping("/user/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    @ApiOperation("微信用户登录")
    public Result<UserLoginVO> login(@RequestBody @Valid UserLoginDTO userLoginDTO) {
        log.info("微信用户登录：{}", userLoginDTO.getCode());
        UserLoginVO userLoginVO = userService.login(userLoginDTO.getCode());
        return Result.success(userLoginVO);
    }

    @GetMapping("/profile")
    @ApiOperation("个人信息（信用分/累计发单数/校区）")
    public Result<User> profile() {
        return Result.success(userService.getProfile(BaseContext.getCurrentId()));
    }
}
