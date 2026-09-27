package com.campus.runner.mapper;

import com.campus.runner.entity.Runner;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface RunnerMapper {

    @Insert("insert into runner (user_id, name, phone, student_no, campus, college, audit_status, runner_level, " +
            "daily_order_limit, completed_orders, score, withdraw_password, status, create_time, update_time) " +
            "values (#{userId}, #{name}, #{phone}, #{studentNo}, #{campus}, #{college}, #{auditStatus}, #{runnerLevel}, " +
            "#{dailyOrderLimit}, #{completedOrders}, #{score}, #{withdrawPassword}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Runner runner);

    @Select("select * from runner where id = #{id}")
    Runner getById(Long id);

    @Select("select * from runner where user_id = #{userId}")
    Runner getByUserId(Long userId);

    int update(Runner runner);

    /**
     * 管理端-跑腿员列表分页
     */
    java.util.List<Runner> page(@Param("name") String name,
                                @Param("campus") String campus,
                                @Param("auditStatus") Integer auditStatus,
                                @Param("status") Integer status);

    @Update("update runner set completed_orders = completed_orders + 1 where id = #{id}")
    void incrementCompletedOrders(Long id);

    @Update("update runner set withdraw_password = #{withdrawPassword}, update_time = now() where id = #{id}")
    void updateWithdrawPassword(@Param("id") Long id, @Param("withdrawPassword") String withdrawPassword);

    @Update("update runner set score = #{score}, update_time = now() where id = #{id}")
    void updateScore(@Param("id") Long id, @Param("score") BigDecimal score);
}
