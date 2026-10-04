package com.campus.runner.service;

import com.campus.runner.vo.CreditRankVO;

import java.util.List;

public interface CreditService {

    /**
     * 记录信用分变动（流水 + 技能者信用分联动加减）
     * @param skillerId 技能者id
     * @param delta 分值变动（正加负减）
     * @param reason 变动原因
     * @param orderId 关联订单id（可为空）
     */
    void addCredit(Long skillerId, int delta, String reason, Long orderId);

    /**
     * 管理端-信用分排行榜（按信用分降序取前 N）
     */
    List<CreditRankVO> getRank(int limit);
}
