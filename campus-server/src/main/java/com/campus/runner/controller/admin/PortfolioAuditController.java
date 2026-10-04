package com.campus.runner.controller.admin;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.PortfolioAuditProcessDTO;
import com.campus.runner.entity.Portfolio;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.PortfolioService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 管理端-作品审核（作品定级联动技能者 skill_level）
 */
@RestController
@Slf4j
@Api(tags = "管理端-作品审核接口")
@RequestMapping("/admin/portfolioAudit")
public class PortfolioAuditController {

    @Autowired
    private PortfolioService portfolioService;

    @GetMapping("/page")
    @ApiOperation("作品审核分页（可按状态过滤）")
    public Result<PageResult<Portfolio>> page(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              Integer status) {
        return Result.success(portfolioService.page(page, pageSize, status));
    }

    @PutMapping("/process")
    @ApiOperation("作品审核处理（1通过 2驳回，通过可评定 C1/C2/C3 定级）")
    public Result<String> process(@RequestBody @Valid PortfolioAuditProcessDTO portfolioAuditProcessDTO) {
        portfolioService.process(portfolioAuditProcessDTO, BaseContext.getCurrentId());
        return Result.success("审核完成");
    }
}
