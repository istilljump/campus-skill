package com.campus.runner.controller.admin;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.DisputeVerdictDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.DisputeService;
import com.campus.runner.service.OrderService;
import com.campus.runner.vo.DisputeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 管理端-纠纷仲裁（查看工单、出具判决）
 */
@RestController
@Slf4j
@Api(tags = "管理端-纠纷仲裁接口")
@RequestMapping("/admin/dispute")
public class DisputeController {

    @Autowired
    private DisputeService disputeService;

    @Autowired
    private OrderService orderService;

    @GetMapping("/page")
    @ApiOperation("仲裁工单分页（可按状态过滤）")
    public Result<PageResult<DisputeVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              Integer status) {
        return Result.success(disputeService.page(page, pageSize, status));
    }

    @PutMapping("/{id}/verdict")
    @ApiOperation("仲裁判决（1退款用户 2放款技能者 3驳回）")
    public Result<String> verdict(@PathVariable Long id, @RequestBody @Valid DisputeVerdictDTO disputeVerdictDTO) {
        orderService.verdictDispute(id, disputeVerdictDTO, BaseContext.getCurrentId());
        return Result.success("仲裁判决已生效");
    }
}
