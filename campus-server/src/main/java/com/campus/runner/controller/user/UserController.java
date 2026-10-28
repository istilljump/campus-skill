package com.campus.runner.controller.user;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.AccountLoginDTO;
import com.campus.runner.dto.AccountRegisterDTO;
import com.campus.runner.dto.PasswordChangeDTO;
import com.campus.runner.dto.UserLoginDTO;
import com.campus.runner.dto.UserUpdateDTO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@Slf4j
@Api(tags = "小程序用户相关接口")
@RequestMapping("/user/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Value("${campus.guest-login.enabled:true}")
    private boolean guestLoginEnabled;

    @PostMapping("/login")
    @ApiOperation("微信用户登录（体验码降级入口，可用 campus.guest-login.enabled 关闭）")
    public Result<UserLoginVO> login(@RequestBody @Valid UserLoginDTO userLoginDTO) {
        if (!guestLoginEnabled) {
            throw new com.campus.runner.exception.BusinessException("体验登录已关闭，请使用账号密码登录");
        }
        log.info("微信用户登录：{}", userLoginDTO.getCode());
        UserLoginVO userLoginVO = userService.login(userLoginDTO.getCode());
        return Result.success(userLoginVO);
    }

    @PostMapping("/register")
    @ApiOperation("账号注册（用户名+密码，成功后直接登录）")
    public Result<UserLoginVO> register(@RequestBody @Valid AccountRegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @PostMapping("/accountLogin")
    @ApiOperation("账号密码登录")
    public Result<UserLoginVO> accountLogin(@RequestBody @Valid AccountLoginDTO dto) {
        return Result.success(userService.loginByAccount(dto));
    }

    @PutMapping("/password")
    @ApiOperation("修改登录密码（需已登录，校验原密码）")
    public Result<String> changePassword(@RequestBody @Valid PasswordChangeDTO dto) {
        userService.changePassword(BaseContext.getCurrentId(), dto);
        return Result.success("密码已更新");
    }

    @PutMapping("/profile")
    @ApiOperation("编辑个人资料（昵称/手机号/学号/校区）")
    public Result<String> updateProfile(@RequestBody @Valid UserUpdateDTO userUpdateDTO) {
        userService.updateProfile(BaseContext.getCurrentId(), userUpdateDTO);
        return Result.success("资料已更新");
    }

    @GetMapping("/profile")
    @ApiOperation("个人信息（信用分/累计发单数/校区）")
    public Result<User> profile() {
        return Result.success(userService.getProfile(BaseContext.getCurrentId()));
    }
}
