package com.campus.runner.mapper;

import com.campus.runner.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserBehaviorMapper {

    void insert(UserBehavior behavior);

    /**
     * 用户最近的去重行为（同一服务多次行为仅保留最新一次），按时间倒序
     */
    List<UserBehavior> listRecentDistinctByUser(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 全量去重行为对（近 N 天，用于构建 ItemCF 共现矩阵），仅取浏览/预约/成交全部用户
     */
    List<UserBehavior> listRecentDistinctAll(@Param("days") Integer days);

    /**
     * 服务维度行为统计（热度）
     */
    List<UserBehavior> listByItem(@Param("serviceItemId") Long serviceItemId, @Param("limit") Integer limit);

    /**
     * 推荐效果统计：近 N 天各行为来源 × 行为类型计数（source/behaviorType/cnt）
     */
    @org.apache.ibatis.annotations.Select("select source as `source`, behavior_type as `behaviorType`, count(*) as `cnt` "
            + "from user_behavior where create_time >= date_sub(now(), interval #{days} day) and source is not null "
            + "group by source, behavior_type")
    List<java.util.Map<String, Object>> countBySource(@Param("days") Integer days);
}
