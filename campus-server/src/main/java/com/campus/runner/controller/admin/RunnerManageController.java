package com.campus.runner.controller.admin;

import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.RunnerService;
import com.campus.runner.vo.RunnerVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-技能者管理
 */
@RestController
@Slf4j
@Api(tags = "管理端-技能者管理接口")
@RequestMapping("/admin/skiller")
public class RunnerManageController {

    @Autowired
    private RunnerService runnerService;

    @GetMapping("/page")
    @ApiOperation("技能者列表分页")
    public Result<PageResult<RunnerVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                             String name, String campus, Integer auditStatus, Integer status) {
        return Result.success(runnerService.page(page, pageSize, name, campus, auditStatus, status));
    }

    @PostMapping("/status/{status}")
    @ApiOperation("启用/禁用技能者")
    public Result<String> updateStatus(@PathVariable Integer status, Long runnerId) {
        runnerService.updateStatus(runnerId, status);
        return Result.success(status == 1 ? "已启用" : "已禁用");
    }
}
