package com.campus.runner.controller.user;

import com.campus.runner.result.Result;
import com.campus.runner.service.ErrandTypeService;
import com.campus.runner.vo.ErrandTypeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端-技能订单类型
 */
@RestController("userErrandTypeController")
@Slf4j
@Api(tags = "用户端-订单类型接口")
@RequestMapping("/user/skillCategory")
public class ErrandTypeController {

    @Autowired
    private ErrandTypeService errandTypeService;

    @GetMapping("/list")
    @ApiOperation("启用中的技能订单类型")
    public Result<List<ErrandTypeVO>> list() {
        return Result.success(errandTypeService.listEnabled());
    }
}
