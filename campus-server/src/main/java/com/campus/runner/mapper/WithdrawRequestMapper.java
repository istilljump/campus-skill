package com.campus.runner.mapper;

import com.campus.runner.entity.WithdrawRequest;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WithdrawRequestMapper {

    @Insert("insert into withdraw_request (user_id, amount, status, apply_time) " +
            "values (#{userId}, #{amount}, #{status}, #{applyTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(WithdrawRequest withdrawRequest);

    @Select("select * from withdraw_request where id = #{id}")
    WithdrawRequest getById(Long id);

    /**
     * 提现申请分页（技能者查自己的，管理端查全部的）
     */
    Page<WithdrawRequest> page(@Param("userId") Long userId, @Param("status") Integer status);

    int update(WithdrawRequest withdrawRequest);
}
