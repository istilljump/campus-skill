package com.campus.runner.controller.runner;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.PortfolioDTO;
import com.campus.runner.entity.Portfolio;
import com.campus.runner.result.Result;
import com.campus.runner.service.PortfolioService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 技能者端-作品集接口（上传/删除作品，作为作品认证材料）
 */
@RestController
@Slf4j
@Api(tags = "技能者端-作品集接口")
@RequestMapping("/skiller/portfolio")
public class SkillerPortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @GetMapping
    @ApiOperation("我的作品列表")
    public Result<List<Portfolio>> list() {
        return Result.success(portfolioService.listOwn(BaseContext.getCurrentId()));
    }

    @PostMapping
    @ApiOperation("上传作品（待审核）")
    public Result<String> create(@RequestBody @Valid PortfolioDTO portfolioDTO) {
        portfolioService.create(BaseContext.getCurrentId(), portfolioDTO);
        return Result.success("作品已上传，等待审核");
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除自己的作品")
    public Result<String> delete(@PathVariable Long id) {
        portfolioService.delete(BaseContext.getCurrentId(), id);
        return Result.success("作品已删除");
    }
}
