package com.campus.runner.service.impl;

import com.campus.runner.constant.MessageConstant;
import com.campus.runner.constant.WalletConstant;
import com.campus.runner.dto.WithdrawApplyDTO;
import com.campus.runner.dto.WithdrawProcessDTO;
import com.campus.runner.entity.Runner;
import com.campus.runner.entity.WalletAccount;
import com.campus.runner.entity.WalletTransaction;
import com.campus.runner.entity.WithdrawRequest;
import com.campus.runner.exception.BusinessException;
import com.campus.runner.exception.InsufficientBalanceException;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.mapper.WalletAccountMapper;
import com.campus.runner.mapper.WalletTransactionMapper;
import com.campus.runner.mapper.WithdrawRequestMapper;
import com.campus.runner.result.PageResult;
import com.campus.runner.service.MessageService;
import com.campus.runner.service.WalletService;
import com.campus.runner.vo.WalletTransactionVO;
import com.campus.runner.vo.WalletVO;
import com.campus.runner.vo.WithdrawRecordVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WalletServiceImpl implements WalletService {

    @Autowired
    private WalletAccountMapper walletAccountMapper;

    @Autowired
    private WalletTransactionMapper walletTransactionMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private WithdrawRequestMapper withdrawRequestMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletVO getWallet(Long userId) {
        WalletAccount wallet = ensureWallet(userId);
        return WalletVO.builder()
                .id(wallet.getId())
                .balance(wallet.getBalance())
                .frozenAmount(wallet.getFrozenAmount())
                .totalIncome(wallet.getTotalIncome())
                .totalExpense(wallet.getTotalExpense())
                .status(wallet.getStatus())
                .updateTime(wallet.getUpdateTime())
                .build();
    }

    @Override
    public PageResult<WalletTransactionVO> pageTransactions(Long userId, Integer page, Integer pageSize, Integer type) {
        WalletAccount wallet = ensureWallet(userId);
        PageHelper.startPage(page, pageSize);
        Page<WalletTransaction> txPage = walletTransactionMapper.page(wallet.getId(), type);
        List<WalletTransactionVO> vos = txPage.getResult().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(txPage.getTotal(), vos, txPage.getPageSize(), txPage.getPageNum());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeBalance(Long userId, BigDecimal delta, Integer type, Long orderId, String remark) {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("金额不能为空");
        }
        WalletAccount wallet = ensureWallet(userId);
        //原子条件更新：余额不足或账户冻结时影响行数为 0
        int rows = walletAccountMapper.adjustBalance(wallet.getId(), delta);
        if (rows == 0) {
            throw new InsufficientBalanceException("余额不足或钱包已冻结");
        }
        //累计收支
        WalletAccount upd = new WalletAccount();
        upd.setId(wallet.getId());
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            upd.setTotalIncome(wallet.getTotalIncome().add(delta));
        } else {
            upd.setTotalExpense(wallet.getTotalExpense().add(delta.negate()));
        }
        walletAccountMapper.update(upd);
        //流水
        WalletTransaction tx = WalletTransaction.builder()
                .walletId(wallet.getId())
                .type(type)
                .amount(delta.abs())
                .orderId(orderId)
                .remark(remark)
                .createTime(LocalDateTime.now())
                .build();
        walletTransactionMapper.insert(tx);
        log.info("钱包余额变动，userId={}, delta={}, type={}, orderId={}", userId, delta, type, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long userId, BigDecimal amount) {
        //模拟充值：实际接入微信支付后由支付回调触发
        changeBalance(userId, amount, WalletConstant.TYPE_RECHARGE, null, "钱包充值");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyWithdraw(Long userId, WithdrawApplyDTO dto) {
        BigDecimal amount = dto.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(MessageConstant.WITHDRAW_AMOUNT_INVALID);
        }
        //提现密码校验
        Runner runner = runnerMapper.getByUserId(userId);
        if (runner == null || runner.getWithdrawPassword() == null) {
            throw new BusinessException(MessageConstant.WITHDRAW_PASSWORD_NOT_SET);
        }
        if (!DigestUtils.md5DigestAsHex(dto.getWithdrawPassword().getBytes())
                .equals(runner.getWithdrawPassword())) {
            throw new BusinessException(MessageConstant.WITHDRAW_PASSWORD_ERROR);
        }
        //余额扣减（冻结效果），失败即余额不足
        changeBalance(userId, amount.negate(), WalletConstant.TYPE_WITHDRAW, null, "提现申请冻结");

        WithdrawRequest request = WithdrawRequest.builder()
                .userId(userId)
                .amount(amount)
                .status(WalletConstant.WITHDRAW_PENDING)
                .applyTime(LocalDateTime.now())
                .build();
        withdrawRequestMapper.insert(request);
        log.info("[操作日志]提现申请，userId={}, amount={}", userId, amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processWithdraw(WithdrawProcessDTO dto) {
        WithdrawRequest request = withdrawRequestMapper.getById(dto.getId());
        if (request == null) {
            throw new BusinessException(MessageConstant.WITHDRAW_NOT_FOUND);
        }
        if (request.getStatus() != WalletConstant.WITHDRAW_PENDING) {
            throw new BusinessException(MessageConstant.WITHDRAW_ALREADY_PROCESSED);
        }
        WithdrawRequest upd = WithdrawRequest.builder()
                .id(request.getId())
                .status(dto.getStatus())
                .auditTime(LocalDateTime.now())
                .remark(dto.getRemark())
                .build();
        if (dto.getStatus() == WalletConstant.WITHDRAW_PAID) {
            //已打款：解冻完成
            upd.setPayTime(LocalDateTime.now());
        } else if (dto.getStatus() == WalletConstant.WITHDRAW_REJECTED) {
            //驳回：退回冻结金额
            changeBalance(request.getUserId(), request.getAmount(),
                    WalletConstant.TYPE_REFUND, null, "提现驳回退回");
        } else {
            throw new BusinessException(MessageConstant.WITHDRAW_AMOUNT_INVALID);
        }
        withdrawRequestMapper.update(upd);
        //站内消息：通知提现处理结果
        boolean paid = dto.getStatus() == WalletConstant.WITHDRAW_PAID;
        messageService.notify(request.getUserId(), paid ? "提现已打款" : "提现申请被驳回",
                paid ? "您申请的提现 ¥" + request.getAmount() + " 已打款，请注意查收"
                        : "您申请的提现 ¥" + request.getAmount() + " 被驳回，金额已退回余额"
                                + (dto.getRemark() != null ? "。原因：" + dto.getRemark() : ""),
                null);
        log.info("[操作日志]提现审核，withdrawId={}, userId={}, result={}, 金额={}, 备注={}",
                dto.getId(), request.getUserId(),
                dto.getStatus() == WalletConstant.WITHDRAW_PAID ? "已打款" : "已驳回", request.getAmount(), dto.getRemark());
    }

    @Override
    public PageResult<WithdrawRecordVO> pageWithdraw(Long userId, Integer page, Integer pageSize, Integer status) {
        PageHelper.startPage(page, pageSize);
        Page<WithdrawRequest> requestPage = withdrawRequestMapper.page(userId, status);
        List<WithdrawRecordVO> vos = requestPage.getResult().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(requestPage.getTotal(), vos, requestPage.getPageSize(), requestPage.getPageNum());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setWithdrawPassword(Long userId, String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new BusinessException("提现密码至少6位");
        }
        Runner runner = runnerMapper.getByUserId(userId);
        if (runner == null) {
            throw new BusinessException(MessageConstant.RUNNER_NOT_FOUND);
        }
        runnerMapper.updateWithdrawPassword(runner.getId(), DigestUtils.md5DigestAsHex(rawPassword.getBytes()));
        log.info("[操作日志]提现密码更新，userId={}", userId);
    }

    /**
     * 钱包懒开户：首次查询时创建
     */
    private WalletAccount ensureWallet(Long userId) {
        WalletAccount wallet = walletAccountMapper.getByUserId(userId);
        if (wallet == null) {
            WalletAccount created = WalletAccount.builder()
                    .userId(userId)
                    .balance(new BigDecimal("0"))
                    .frozenAmount(new BigDecimal("0"))
                    .totalIncome(new BigDecimal("0"))
                    .totalExpense(new BigDecimal("0"))
                    .status(WalletConstant.ACCOUNT_NORMAL)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            walletAccountMapper.insert(created);
            wallet = walletAccountMapper.getByUserId(userId);
        }
        return wallet;
    }

    private WalletTransactionVO toVO(WalletTransaction tx) {
        return WalletTransactionVO.builder()
                .id(tx.getId())
                .type(tx.getType())
                .amount(tx.getAmount())
                .orderId(tx.getOrderId())
                .remark(tx.getRemark())
                .createTime(tx.getCreateTime())
                .build();
    }

    private WithdrawRecordVO toVO(WithdrawRequest request) {
        return WithdrawRecordVO.builder()
                .id(request.getId())
                .amount(request.getAmount())
                .status(request.getStatus())
                .applyTime(request.getApplyTime())
                .payTime(request.getPayTime())
                .remark(request.getRemark())
                .build();
    }
}
