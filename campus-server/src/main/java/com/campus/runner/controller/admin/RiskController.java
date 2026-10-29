package com.campus.runner.controller.admin;

import com.campus.runner.dto.RiskHandleDTO;
import com.campus.runner.entity.RiskControlLog;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.RiskControlService;
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
 * 管理端-风控中心：告警查询与处置
 */
@RestController
@Slf4j
@Api(tags = "管理端-风控中心接口")
@RequestMapping("/admin/risk")
public class RiskController {

    @Autowired
    private RiskControlService riskControlService;

    @GetMapping("/page")
    @ApiOperation("风控告警分页（状态/等级筛选）")
    public Result<PageResult<RiskControlLog>> page(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   Integer status, Integer level) {
        return Result.success(riskControlService.page(status, level, page, pageSize));
    }

    @PutMapping("/handle")
    @ApiOperation("处置告警（标记已处置 + 备注）")
    public Result<String> handle(@RequestBody @Valid RiskHandleDTO dto) {
        riskControlService.handle(dto.getId(), dto.getRemark());
        return Result.success("处置完成");
    }
}
