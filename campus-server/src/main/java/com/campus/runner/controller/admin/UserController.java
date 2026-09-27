package com.campus.runner.controller.admin;

import com.campus.runner.entity.User;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-用户管理
 */
@RestController("adminUserController")
@Slf4j
@Api(tags = "管理端-用户管理接口")
@RequestMapping("/admin/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/page")
    @ApiOperation("用户列表分页")
    public Result<PageResult<User>> page(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         String name, String phone, String campus) {
        return Result.success(userService.page(page, pageSize, name, phone, campus));
    }
}
