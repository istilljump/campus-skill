package com.campus.runner.mapper;

import com.campus.runner.entity.Dispute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DisputeMapper {

    void insert(Dispute dispute);

    Dispute getById(Long id);

    /**
     * 管理端-仲裁工单分页（status 为空查全部；PageHelper 在 service 层调用 startPage）
     */
    List<Dispute> listByStatus(@Param("status") Integer status);

    void updateStatusVerdict(@Param("id") Long id,
                             @Param("status") Integer status,
                             @Param("verdict") String verdict,
                             @Param("adminId") Long adminId);
}
