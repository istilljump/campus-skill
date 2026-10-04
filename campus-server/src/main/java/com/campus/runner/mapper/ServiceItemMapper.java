package com.campus.runner.mapper;

import com.campus.runner.entity.ServiceItem;
import com.campus.runner.vo.ServiceItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ServiceItemMapper {

    void insert(ServiceItem serviceItem);

    int update(ServiceItem serviceItem);

    ServiceItem getById(Long id);

    List<ServiceItem> listBySkiller(Long skillerId);

    /**
     * 用户端-服务市场分页（仅上架，按销量、评分排序；PageHelper 在 service 层调用 startPage）
     */
    List<ServiceItemVO> listOnShelf(@Param("categoryId") Long categoryId, @Param("keyword") String keyword);

    /**
     * 用户端-服务详情（含技能者/类目冗余信息）
     */
    ServiceItemVO getVOById(@Param("id") Long id);

    @Update("update service_item set sales_count = sales_count + 1, update_time = now() where id = #{id}")
    void incrementSales(Long id);

    @Update("update service_item set avg_score = #{score}, update_time = now() where id = #{id}")
    void updateAvgScore(@Param("id") Long id, @Param("score") BigDecimal score);
}
