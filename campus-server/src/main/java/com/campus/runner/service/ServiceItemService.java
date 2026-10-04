package com.campus.runner.service;

import com.campus.runner.dto.ServiceItemDTO;
import com.campus.runner.entity.ServiceItem;
import com.campus.runner.result.PageResult;
import com.campus.runner.vo.ServiceItemVO;

import java.util.List;

public interface ServiceItemService {

    /**
     * 技能者端-我的服务列表
     */
    List<ServiceItem> listOwn(Long skillerId);

    /**
     * 技能者端-发布服务（默认上架）
     */
    Long create(Long skillerId, ServiceItemDTO serviceItemDTO);

    /**
     * 技能者端-编辑自己的服务
     */
    void update(Long skillerId, Long id, ServiceItemDTO serviceItemDTO);

    /**
     * 技能者端-上架/下架自己的服务
     */
    void updateStatus(Long skillerId, Long id, Integer status);

    /**
     * 用户端-服务市场分页（仅上架）
     */
    PageResult<ServiceItemVO> pageOnShelf(Integer page, Integer pageSize, Long categoryId, String keyword, String sort);

    /**
     * 用户端-服务详情
     */
    ServiceItemVO getVOById(Long id);

    /**
     * 实体转 VO（补技能者/类目冗余信息）
     */
    ServiceItemVO toVO(ServiceItem serviceItem);
}
