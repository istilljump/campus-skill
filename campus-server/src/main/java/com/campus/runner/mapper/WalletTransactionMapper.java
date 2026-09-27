package com.campus.runner.mapper;

import com.campus.runner.entity.WalletTransaction;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WalletTransactionMapper {

    @Insert("insert into wallet_transaction (wallet_id, type, amount, order_id, remark, create_time) " +
            "values (#{walletId}, #{type}, #{amount}, #{orderId}, #{remark}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(WalletTransaction transaction);

    /**
     * 钱包流水分页
     */
    Page<WalletTransaction> page(@Param("walletId") Long walletId, @Param("type") Integer type);
}
