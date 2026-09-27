package com.campus.runner.service;

import com.campus.runner.dto.RunnerAuditProcessDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.vo.RunnerAuditVO;

public interface RunnerAuditService {

    /**
     * 管理端-审核记录分页
     */
    PageResult<RunnerAuditVO> page(Integer page, Integer pageSize, Integer status, String studentNo);

    /**
     * 管理端-审核处理：更新审核记录并联动跑腿员认证状态
     */
    void process(RunnerAuditProcessDTO runnerAuditProcessDTO);
}
