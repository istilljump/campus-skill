package com.campus.runner.service;

import com.campus.runner.entity.WalletAccount;
import com.campus.runner.exception.InsufficientBalanceException;
import com.campus.runner.mapper.WalletAccountMapper;
import com.campus.runner.mapper.WalletTransactionMapper;
import com.campus.runner.mapper.WithdrawRequestMapper;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.constant.WalletConstant;
import com.campus.runner.service.impl.WalletServiceImpl;
import com.campus.runner.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 钱包资金链路单元测试（Mockito，不依赖数据库）
 *
 * <p>覆盖：懒开户、余额变动记账（收入/支出方向）、余额不足拒绝、
 * 资金托管支付的扣款语义。这些是担保交易资金安全的核心不变量。
 */
class WalletServiceTest {

    private WalletAccountMapper walletAccountMapper;
    private WalletTransactionMapper walletTransactionMapper;
    private WalletServiceImpl walletService;
    private WalletAccount account;

    @BeforeEach
    void setUp() {
        walletAccountMapper = mock(WalletAccountMapper.class);
        walletTransactionMapper = mock(WalletTransactionMapper.class);
        WalletServiceImpl impl = new WalletServiceImpl();
        //通过反射注入依赖（避免仅为测试引入 Setter）
        org.springframework.test.util.ReflectionTestUtils.setField(impl, "walletAccountMapper", walletAccountMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(impl, "walletTransactionMapper", walletTransactionMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(impl, "messageService", mock(MessageService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(impl, "withdrawRequestMapper", mock(WithdrawRequestMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(impl, "runnerMapper", mock(RunnerMapper.class));
        walletService = impl;

        account = WalletAccount.builder()
                .id(1L).userId(100L).balance(new BigDecimal("100.00"))
                .totalIncome(new BigDecimal("100.00")).totalExpense(BigDecimal.ZERO)
                .status(1).build();
        when(walletAccountMapper.getByUserId(100L)).thenReturn(account);
    }

    @Test
    @DisplayName("余额变动：金额为空或 0 直接拒绝")
    void changeBalance_rejectsZeroOrNull() {
        assertThrows(Exception.class, () -> walletService.changeBalance(100L, null, WalletConstant.TYPE_RECHARGE, null, "t"));
        assertThrows(Exception.class, () -> walletService.changeBalance(100L, BigDecimal.ZERO, WalletConstant.TYPE_RECHARGE, null, "t"));
        verify(walletAccountMapper, never()).adjustBalance(anyLong(), any());
    }

    @Test
    @DisplayName("余额不足：条件更新影响行数为 0 时抛 InsufficientBalanceException 且不写流水")
    void changeBalance_insufficientBalance() {
        when(walletAccountMapper.adjustBalance(eq(1L), any())).thenReturn(0);
        assertThrows(InsufficientBalanceException.class, () ->
                walletService.changeBalance(100L, new BigDecimal("-999.00"), WalletConstant.TYPE_EXPENSE, 9L, "超额支出"));
        verify(walletTransactionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("收入方向：余额原子变更 + 累计收入更新 + 流水金额取绝对值")
    void changeBalance_incomeDirection() {
        when(walletAccountMapper.adjustBalance(eq(1L), any())).thenReturn(1);
        walletService.changeBalance(100L, new BigDecimal("50.00"), WalletConstant.TYPE_RECHARGE, null, "充值");

        verify(walletAccountMapper).adjustBalance(eq(1L), eq(new BigDecimal("50.00")));
        ArgumentCaptor<WalletAccount> captor = ArgumentCaptor.forClass(WalletAccount.class);
        verify(walletAccountMapper).update(captor.capture());
        assertEquals(new BigDecimal("150.00"), captor.getValue().getTotalIncome());

        ArgumentCaptor<com.campus.runner.entity.WalletTransaction> txCaptor =
                ArgumentCaptor.forClass(com.campus.runner.entity.WalletTransaction.class);
        verify(walletTransactionMapper).insert(txCaptor.capture());
        assertEquals(new BigDecimal("50.00"), txCaptor.getValue().getAmount());
    }

    @Test
    @DisplayName("支出方向：累计支出按负数绝对值累计")
    void changeBalance_expenseDirection() {
        when(walletAccountMapper.adjustBalance(eq(1L), any())).thenReturn(1);
        walletService.changeBalance(100L, new BigDecimal("-30.00"), WalletConstant.TYPE_EXPENSE, 7L, "托管支付");

        verify(walletAccountMapper).adjustBalance(eq(1L), eq(new BigDecimal("-30.00")));
        ArgumentCaptor<WalletAccount> captor = ArgumentCaptor.forClass(WalletAccount.class);
        verify(walletAccountMapper).update(captor.capture());
        assertEquals(new BigDecimal("30.00"), captor.getValue().getTotalExpense());
    }

    @Test
    @DisplayName("懒开户：首次操作时账户不存在则通过 Redis 锁串行创建")
    void ensureWallet_lazyCreation() {
        WalletAccount created = WalletAccount.builder()
                .id(2L).userId(200L).balance(BigDecimal.ZERO)
                .totalIncome(BigDecimal.ZERO).totalExpense(BigDecimal.ZERO).status(1).build();
        when(walletAccountMapper.getByUserId(200L)).thenReturn(null);
        when(walletAccountMapper.getByUserIdForUpdate(200L)).thenReturn(null).thenReturn(created);
        when(walletAccountMapper.adjustBalance(eq(2L), any())).thenReturn(1);
        org.springframework.data.redis.core.ValueOperations<String, String> vo =
                org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        org.springframework.data.redis.core.StringRedisTemplate redis =
                org.mockito.Mockito.mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        when(redis.opsForValue()).thenReturn(vo);
        when(vo.setIfAbsent(any(), any(), org.mockito.ArgumentMatchers.any(java.time.Duration.class))).thenReturn(true);
        org.springframework.test.util.ReflectionTestUtils.setField(walletService, "stringRedisTemplate", redis);

        walletService.changeBalance(200L, new BigDecimal("10.00"), WalletConstant.TYPE_RECHARGE, null, "首充");
        verify(walletAccountMapper).insert(any(WalletAccount.class));
    }
}
