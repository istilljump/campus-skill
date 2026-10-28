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
    public com.campus.runner.vo.SkillerCreditVO myCredit(Long skillerId) {
        com.campus.runner.entity.Runner runner = runnerMapper.getById(skillerId);
        int score = runner == null || runner.getCreditScore() == null ? 100 : runner.getCreditScore();
        //等级口径与 README 一致：C3≥90 / C2≥70 / C1<70
        String level = score >= 90 ? "C3" : score >= 70 ? "C2" : "C1";
        String levelDesc = "C3".equals(level) ? "信用优秀，可获更多曝光与信任"
                : "C2".equals(level) ? "信用良好，保持按时交付可升级 C3"
                : "信用待提升，避免返修与超时";
        Integer toNext = "C3".equals(level) ? null : ("C2".equals(level) ? 90 - score : 70 - score);
        return com.campus.runner.vo.SkillerCreditVO.builder()
                .creditScore(score)
                .level(level)
                .levelDesc(levelDesc)
                .toNextLevel(toNext)
                .logs(creditLogMapper.listBySkiller(skillerId))
                .build();
    }

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
