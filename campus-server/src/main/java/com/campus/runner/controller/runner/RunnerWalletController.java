package com.campus.runner.controller.runner;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.WithdrawApplyDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.RunnerService;
import com.campus.runner.service.WalletService;
import com.campus.runner.vo.WalletTransactionVO;
import com.campus.runner.vo.WalletVO;
import com.campus.runner.vo.WithdrawRecordVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 技能者端-钱包接口（余额、流水、提现）
 */
@RestController
@Slf4j
@Api(tags = "技能者端-钱包接口")
@RequestMapping("/skiller/wallet")
public class RunnerWalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private RunnerService runnerService;

    @GetMapping("/balance")
    @ApiOperation("钱包余额查询")
    public Result<WalletVO> balance() {
        return Result.success(walletService.getWallet(currentUserId()));
    }

    @GetMapping("/transactions")
    @ApiOperation("钱包流水分页")
    public Result<PageResult<WalletTransactionVO>> transactions(@RequestParam(defaultValue = "1") Integer page,
                                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                                Integer type) {
        return Result.success(walletService.pageTransactions(currentUserId(), page, pageSize, type));
    }

    @PostMapping("/withdraw")
    @ApiOperation("提现申请")
    public Result<String> withdraw(@RequestBody @Valid WithdrawApplyDTO withdrawApplyDTO) {
        walletService.applyWithdraw(currentUserId(), withdrawApplyDTO);
        return Result.success("提现申请已提交");
    }

    @GetMapping("/withdraw/page")
    @ApiOperation("提现记录分页")
    public Result<PageResult<WithdrawRecordVO>> withdrawPage(@RequestParam(defaultValue = "1") Integer page,
                                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                                             Integer status) {
        return Result.success(walletService.pageWithdraw(currentUserId(), page, pageSize, status));
    }

    @PostMapping("/password")
    @ApiOperation("设置/修改提现密码")
    public Result<String> setPassword(@RequestParam String password) {
        walletService.setWithdrawPassword(currentUserId(), password);
        return Result.success("提现密码已更新");
    }

    /**
     * 钱包按用户维度记账，这里把技能者id解析为用户id
     */
    private Long currentUserId() {
        return runnerService.resolveUserId(BaseContext.getCurrentId());
    }
}
