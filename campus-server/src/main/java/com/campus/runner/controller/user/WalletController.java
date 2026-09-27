package com.campus.runner.controller.user;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.WalletRechargeDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.WalletService;
import com.campus.runner.vo.WalletTransactionVO;
import com.campus.runner.vo.WalletVO;
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
 * 用户端-钱包接口
 */
@RestController
@Slf4j
@Api(tags = "用户端-钱包接口")
@RequestMapping("/user/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @GetMapping("/balance")
    @ApiOperation("钱包余额查询")
    public Result<WalletVO> balance() {
        return Result.success(walletService.getWallet(BaseContext.getCurrentId()));
    }

    @GetMapping("/transactions")
    @ApiOperation("钱包流水分页")
    public Result<PageResult<WalletTransactionVO>> transactions(@RequestParam(defaultValue = "1") Integer page,
                                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                                Integer type) {
        return Result.success(walletService.pageTransactions(BaseContext.getCurrentId(), page, pageSize, type));
    }

    @PostMapping("/recharge")
    @ApiOperation("钱包充值")
    public Result<String> recharge(@RequestBody @Valid WalletRechargeDTO walletRechargeDTO) {
        walletService.recharge(BaseContext.getCurrentId(), walletRechargeDTO.getAmount());
        return Result.success("充值成功");
    }
}
