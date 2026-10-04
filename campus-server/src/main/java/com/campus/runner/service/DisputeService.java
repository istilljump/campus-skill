package com.campus.runner.service;

import com.campus.runner.result.PageResult;
import com.campus.runner.vo.DisputeVO;

public interface DisputeService {

    /**
     * 管理端-仲裁工单分页（可按状态过滤，含订单/用户冗余信息）
     */
    PageResult<DisputeVO> page(Integer page, Integer pageSize, Integer status);
}
