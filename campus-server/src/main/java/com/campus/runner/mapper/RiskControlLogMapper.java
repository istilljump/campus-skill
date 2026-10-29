package com.campus.runner.mapper;

import com.campus.runner.entity.RiskControlLog;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RiskControlLogMapper {

    @Insert("insert into risk_control_log (rule_code, rule_name, risk_level, target_type, target_id, order_id, detail, status, create_time) " +
            "values (#{ruleCode}, #{ruleName}, #{riskLevel}, #{targetType}, #{targetId}, #{orderId}, #{detail}, 0, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RiskControlLog log);

    /**
     * 风控告警分页（状态/等级可选，最新在前）
     */
    Page<RiskControlLog> page(@Param("status") Integer status, @Param("level") Integer level);

    @Select("select * from risk_control_log where id = #{id}")
    RiskControlLog getById(Long id);

    @Update("update risk_control_log set status = 1, handle_remark = #{remark}, handle_time = now() " +
            "where id = #{id} and status = 0")
    int markHandled(@Param("id") Long id, @Param("remark") String remark);

    // ---------- 规则引擎统计查询 ----------

    /**
     * 用户最近 N 分钟发单数（R001 高频发单）
     */
    @Select("select count(*) from orders where user_id = #{userId} " +
            "and create_time > date_sub(now(), interval #{minutes} minute)")
    int countRecentOrders(@Param("userId") Long userId, @Param("minutes") int minutes);

    /**
     * 用户当日提现申请笔数（R003 高频提现）
     */
    @Select("select count(*) from withdraw_request where user_id = #{userId} and apply_time >= curdate()")
    int countTodayWithdraw(@Param("userId") Long userId);

    /**
     * 技能者最近 window 单中返修单数（R004 返修率异常）
     */
    @Select("select count(*) from (select rework_count from orders where runner_id = #{skillerId} " +
            "and status in (4, 5, 8) order by id desc limit #{window}) t where t.rework_count > 0")
    int countRecentReworks(@Param("skillerId") Long skillerId, @Param("window") int window);
}
