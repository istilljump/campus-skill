package com.campus.runner.service;

import com.campus.runner.dto.WithdrawApplyDTO;
import com.campus.runner.dto.WithdrawProcessDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.vo.WalletTransactionVO;
import com.campus.runner.vo.WalletVO;
import com.campus.runner.vo.WithdrawRecordVO;

import java.math.BigDecimal;

public interface WalletService {

    /**
     * 余额查询（钱包不存在时自动开户）
     */
    WalletVO getWallet(Long userId);

    /**
     * 钱包流水分页
     */
    PageResult<WalletTransactionVO> pageTransactions(Long userId, Integer page, Integer pageSize, Integer type);

    /**
     * 统一余额变动：原子加减 + 累计收支更新 + 流水记录
     * @param delta 正数为入账，负数为出账
     */
    void changeBalance(Long userId, BigDecimal delta, Integer type, Long orderId, String remark);

    /**
     * 钱包充值（模拟支付）
     */
    void recharge(Long userId, BigDecimal amount);

    /**
     * 跑腿员端-提现申请（余额冻结，等待审核）
     */
    void applyWithdraw(Long userId, WithdrawApplyDTO withdrawApplyDTO);

    /**
     * 管理端-提现审核（打款/驳回）
     */
    void processWithdraw(WithdrawProcessDTO withdrawProcessDTO);

    /**
     * 提现记录分页
     */
    PageResult<WithdrawRecordVO> pageWithdraw(Long userId, Integer page, Integer pageSize, Integer status);

    /**
     * 设置/修改提现密码
     */
    void setWithdrawPassword(Long userId, String rawPassword);
}
