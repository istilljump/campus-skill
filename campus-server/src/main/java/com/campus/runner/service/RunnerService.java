package com.campus.runner.service;

import com.campus.runner.dto.RunnerAuditApplyDTO;
import com.campus.runner.result.PageResult;
import com.campus.runner.vo.RunnerCenterVO;
import com.campus.runner.vo.RunnerVO;
import com.campus.runner.vo.TrendPointVO;
import com.campus.runner.vo.UserLoginVO;

import java.util.List;

public interface RunnerService {

    /**
     * 跑腿员端-登录（复用用户登录，token 中附加跑腿员身份）
     */
    UserLoginVO login(String code);

    /**
     * 用户端-申请成为跑腿员/提交认证
     */
    void applyAudit(Long userId, RunnerAuditApplyDTO runnerAuditApplyDTO);

    /**
     * 跑腿员端-个人中心（含今日接单数、今日收入、钱包余额）
     */
    RunnerCenterVO getCenter(Long userId);

    /**
     * 跑腿员端-近 days 日完成单量与收入趋势（缺失日期补零）
     */
    List<TrendPointVO> dailyTrend(Long userId, int days);

    /**
     * 根据完成订单数自动更新跑腿等级
     */
    void updateLevel(Long runnerId);

    /**
     * 根据评价重算综合评分
     */
    void updateScore(Long runnerId);

    /**
     * 接单权限校验：已认证、账号启用、未达每日接单上限
     */
    void checkGrabPermission(Long runnerId);

    /**
     * 根据跑腿员id解析关联用户id（钱包等按用户维度记账）
     */
    Long resolveUserId(Long runnerId);

    /**
     * 管理端-启用/禁用跑腿员
     */
    void updateStatus(Long runnerId, Integer status);

    /**
     * 管理端-跑腿员列表分页
     */
    PageResult<RunnerVO> page(Integer page, Integer pageSize, String name, String campus, Integer auditStatus, Integer status);
}
