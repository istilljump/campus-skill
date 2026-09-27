package com.campus.runner.mapper;

import com.campus.runner.entity.WalletAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WalletAccountMapper {

    @Insert("insert into wallet_account (user_id, balance, frozen_amount, total_income, total_expense, status, create_time, update_time) " +
            "values (#{userId}, 0, 0, 0, 0, 1, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(WalletAccount walletAccount);

    @Select("select * from wallet_account where user_id = #{userId}")
    WalletAccount getByUserId(Long userId);

    @Select("select * from wallet_account where id = #{id}")
    WalletAccount getById(Long id);

    /**
     * 行级锁查询（转账/扣款场景使用）
     */
    @Select("select * from wallet_account where id = #{id} for update")
    WalletAccount getByIdForUpdate(Long id);

    int update(WalletAccount walletAccount);

    /**
     * 余额原子变更：仅当余额充足时执行
     * @return 影响行数，0 表示余额不足
     */
    @Update("update wallet_account set balance = balance + #{delta}, update_time = now() " +
            "where id = #{id} and status = 1 and balance + #{delta} >= 0")
    int adjustBalance(@Param("id") Long id, @Param("delta") java.math.BigDecimal delta);
}
