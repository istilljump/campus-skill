package com.campus.runner.mapper;

import com.campus.runner.entity.OrderReview;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderReviewMapper {

    @Insert("insert into order_review (order_id, user_id, runner_id, score, content, tags, is_anonymous, create_time) " +
            "values (#{orderId}, #{userId}, #{runnerId}, #{score}, #{content}, #{tags}, #{isAnonymous}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(OrderReview review);

    @Select("select * from order_review where id = #{id}")
    OrderReview getById(Long id);

    @Select("select * from order_review where order_id = #{orderId}")
    OrderReview getByOrderId(Long orderId);

    /**
     * 技能者收到的评价分页
     */
    Page<OrderReview> pageByRunnerId(@Param("runnerId") Long runnerId);

    /**
     * 重算技能者综合评分
     */
    @Select("select ifnull(round(avg(score), 1), 5.0) from order_review where runner_id = #{runnerId}")
    java.math.BigDecimal avgScoreByRunnerId(Long runnerId);
}
