package com.campus.runner.service;

import com.campus.runner.dto.ErrandTypeDTO;
import com.campus.runner.vo.ErrandTypeVO;

import java.util.List;

public interface ErrandTypeService {

    /**
     * 用户端/技能者端-启用中的订单类型
     */
    List<ErrandTypeVO> listEnabled();

    /**
     * 管理端-全部订单类型
     */
    List<ErrandTypeVO> listAll();

    /**
     * 管理端-新增订单类型
     */
    void save(ErrandTypeDTO errandTypeDTO);

    /**
     * 管理端-修改订单类型
     */
    void update(ErrandTypeDTO errandTypeDTO);

    /**
     * 管理端-启用/停用
     */
    void updateStatus(Long id, Integer status);

    /**
     * 管理端-删除订单类型（有关联订单时禁止删除，提示改用停用）
     */
    void delete(Long id);
}
