package com.campus.runner.service.impl;

import com.campus.runner.entity.CreditLog;
import com.campus.runner.mapper.CreditLogMapper;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.service.CreditService;
import com.campus.runner.vo.CreditRankVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @CacheEvict(cacheNames = com.campus.runner.constant.CacheNames.CREDIT_RANK, allEntries = true)
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
    @Cacheable(cacheNames = com.campus.runner.constant.CacheNames.CREDIT_RANK, key = "#limit")
    public List<CreditRankVO> getRank(int limit) {
        limit = Math.max(1, Math.min(limit, 100));
        return creditLogMapper.getRank(limit);
    }

    @Override
    public void exportProfile(Long skillerId, java.io.OutputStream out) throws java.io.IOException {
        com.campus.runner.entity.Runner runner = runnerMapper.getById(skillerId);
        com.campus.runner.vo.SkillerCreditVO credit = myCredit(skillerId);
        org.apache.poi.xssf.streaming.SXSSFWorkbook workbook = new org.apache.poi.xssf.streaming.SXSSFWorkbook();
        try {
            //第一页：档案概要（身份 + 当前等级 + 汇总统计）
            int plus = credit.getLogs() == null ? 0 : credit.getLogs().stream().mapToInt(l -> Math.max(0, l.getDelta())).sum();
            int minus = credit.getLogs() == null ? 0 : credit.getLogs().stream().mapToInt(l -> Math.min(0, l.getDelta())).sum();
            List<List<Object>> summary = java.util.Arrays.asList(
                    java.util.Arrays.<Object>asList("技能者", runner != null ? runner.getName() : String.valueOf(skillerId)),
                    java.util.Arrays.<Object>asList("校区", runner != null ? runner.getCampus() : ""),
                    java.util.Arrays.<Object>asList("学院", runner != null ? runner.getCollege() : ""),
                    java.util.Arrays.<Object>asList("当前信用分", credit.getCreditScore()),
                    java.util.Arrays.<Object>asList("信用等级", credit.getLevel()),
                    java.util.Arrays.<Object>asList("等级说明", credit.getLevelDesc()),
                    java.util.Arrays.<Object>asList("综合评分", runner != null ? runner.getScore() : null),
                    java.util.Arrays.<Object>asList("累计完成订单", runner != null ? runner.getCompletedOrders() : null),
                    java.util.Arrays.<Object>asList("流水加分合计（近50条）", plus),
                    java.util.Arrays.<Object>asList("流水扣分合计（近50条）", minus),
                    java.util.Arrays.<Object>asList("导出时间", java.time.LocalDateTime.now()));
            com.campus.runner.utils.ExcelWriterUtil.writeSheet(workbook, "档案概要",
                    java.util.Arrays.asList("项目", "内容"), summary);

            //第二页：流水明细溯源（每笔分值变动可关联到订单）
            List<List<Object>> rows = new java.util.ArrayList<>();
            if (credit.getLogs() != null) {
                for (com.campus.runner.vo.CreditLogVO l : credit.getLogs()) {
                    rows.add(java.util.Arrays.<Object>asList(l.getCreateTime(), l.getDelta(), l.getReason(),
                            l.getOrderId(), l.getOrderNumber()));
                }
            }
            com.campus.runner.utils.ExcelWriterUtil.writeSheet(workbook, "信用流水明细",
                    java.util.Arrays.asList("时间", "分值变动", "原因", "订单ID", "订单号"), rows);

            workbook.write(out);
        } finally {
            workbook.dispose();
        }
    }
}
