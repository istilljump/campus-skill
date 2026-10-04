package com.campus.runner.mapper;

import com.campus.runner.entity.Portfolio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PortfolioMapper {

    void insert(Portfolio portfolio);

    Portfolio getById(Long id);

    List<Portfolio> listBySkiller(Long skillerId);

    /**
     * 管理端-待审核作品分页（PageHelper 在 service 层调用 startPage）
     */
    List<Portfolio> listPending(@Param("status") Integer status);

    void updateStatus(@Param("id") Long id,
                      @Param("status") Integer status,
                      @Param("auditOpinion") String auditOpinion,
                      @Param("auditorId") Long auditorId);

    int deleteByIdAndSkiller(@Param("id") Long id, @Param("skillerId") Long skillerId);
}
