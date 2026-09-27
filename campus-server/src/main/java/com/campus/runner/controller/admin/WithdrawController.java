package com.campus.runner.controller.admin;

import com.campus.runner.dto.WithdrawProcessDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.WalletService;
import com.campus.runner.vo.WithdrawRecordVO;
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
 * 管理端-提现审核
 */
@RestController
@Slf4j
@Api(tags = "管理端-提现审核接口")
@RequestMapping("/admin/withdraw")
public class WithdrawController {

    @Autowired
    private WalletService walletService;

    @GetMapping("/page")
    @ApiOperation("提现申请分页")
    public Result<PageResult<WithdrawRecordVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                     Integer status) {
        return Result.success(walletService.pageWithdraw(null, page, pageSize, status));
    }

    @PutMapping("/process")
    @ApiOperation("提现处理（打款/驳回）")
    public Result<String> process(@RequestBody @Valid WithdrawProcessDTO withdrawProcessDTO) {
        walletService.processWithdraw(withdrawProcessDTO);
        return Result.success("处理完成");
    }
}
