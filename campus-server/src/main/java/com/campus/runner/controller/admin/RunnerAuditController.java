package com.campus.runner.controller.admin;

import com.campus.runner.dto.RunnerAuditProcessDTO;
import com.campus.runner.entity.RunnerAudit;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.RunnerAuditService;
import com.campus.runner.vo.RunnerAuditVO;
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
 * 管理端-跑腿员认证审核
 */
@RestController
@Slf4j
@Api(tags = "管理端-认证审核接口")
@RequestMapping("/admin/runnerAudit")
public class RunnerAuditController {

    @Autowired
    private RunnerAuditService runnerAuditService;

    @GetMapping("/page")
    @ApiOperation("审核记录分页")
    public Result<PageResult<RunnerAuditVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  Integer status, String studentNo) {
        return Result.success(runnerAuditService.page(page, pageSize, status, studentNo));
    }

    @PutMapping("/process")
    @ApiOperation("审核处理（通过/驳回）")
    public Result<String> process(@RequestBody @Valid RunnerAuditProcessDTO runnerAuditProcessDTO) {
        runnerAuditService.process(runnerAuditProcessDTO);
        return Result.success("审核完成");
    }
}
