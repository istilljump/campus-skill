package com.campus.runner.controller.user;

import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.ServiceItemService;
import com.campus.runner.vo.ServiceItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端-技能市场接口（浏览上架服务）
 */
@RestController
@Slf4j
@Api(tags = "用户端-技能市场接口")
@RequestMapping("/user/market")
public class MarketController {

    @Autowired
    private ServiceItemService serviceItemService;

    @GetMapping
    @ApiOperation("服务市场分页（仅上架，支持类目/关键词筛选）")
    public Result<PageResult<ServiceItemVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  Long categoryId, String keyword) {
        return Result.success(serviceItemService.pageOnShelf(page, pageSize, categoryId, keyword));
    }

    @GetMapping("/{id}")
    @ApiOperation("服务详情")
    public Result<ServiceItemVO> detail(@PathVariable Long id) {
        return Result.success(serviceItemService.getVOById(id));
    }
}
