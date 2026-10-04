package com.campus.runner.controller.user;

import com.campus.runner.result.Result;
import com.campus.runner.service.PortfolioService;
import com.campus.runner.vo.SkillerProfileVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端-技能者主页接口（公开信息、作品集、上架服务）
 */
@RestController
@Slf4j
@Api(tags = "用户端-技能者主页接口")
@RequestMapping("/user/skiller")
public class SkillerProfileController {

    @Autowired
    private PortfolioService portfolioService;

    @GetMapping("/{id}")
    @ApiOperation("技能者主页")
    public Result<SkillerProfileVO> profile(@PathVariable Long id) {
        return Result.success(portfolioService.getSkillerProfile(id));
    }
}
