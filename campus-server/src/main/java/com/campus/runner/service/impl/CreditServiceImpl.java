package com.campus.runner.service.impl;

import com.campus.runner.entity.CreditLog;
import com.campus.runner.mapper.CreditLogMapper;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.service.CreditService;
import com.campus.runner.vo.CreditRankVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CreditServiceImpl implements CreditService {

    @Autowired
    private CreditLogMapper creditLogMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCredit(Long skillerId, int delta, String reason, Long orderId) {
        if (skillerId == null) {
            return;
        }
        creditLogMapper.insert(CreditLog.builder()
                .skillerId(skillerId)
                .delta(delta)
                .reason(reason)
                .orderId(orderId)
                .createTime(LocalDateTime.now())
                .build());
        runnerMapper.adjustCreditScore(skillerId, delta);
        log.info("技能者信用分变动，skillerId={}, delta={}, reason={}, orderId={}", skillerId, delta, reason, orderId);
    }

    @Override
    public List<CreditRankVO> getRank(int limit) {
        limit = Math.max(1, Math.min(limit, 100));
        return creditLogMapper.getRank(limit);
    }
}
