package com.campus.runner.mapper;

import com.campus.runner.entity.CreditLog;
import com.campus.runner.vo.CreditRankVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CreditLogMapper {

    @Insert("insert into credit_log (skiller_id, delta, reason, order_id, create_time) " +
            "values (#{skillerId}, #{delta}, #{reason}, #{orderId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CreditLog creditLog);

    List<CreditLog> listBySkiller(Long skillerId);

    /**
     * 信用分排行榜（按信用分降序取前 N）
     */
    @Select("select s.id as skillerId, s.name as name, s.credit_score as creditScore, " +
            "s.skill_level as skillLevel, s.score as score, s.completed_orders as completedOrders " +
            "from skiller s order by s.credit_score desc, s.score desc limit #{limit}")
    List<CreditRankVO> getRank(@Param("limit") int limit);
}
