package com.campus.runner.service.impl;

import com.campus.runner.constant.MessageConstant;
import com.campus.runner.constant.RunnerConstant;
import com.campus.runner.dto.RunnerAuditProcessDTO;
import com.campus.runner.entity.Runner;
import com.campus.runner.entity.RunnerAudit;
import com.campus.runner.exception.BusinessException;
import com.campus.runner.mapper.RunnerAuditMapper;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.mapper.UserMapper;
import com.campus.runner.result.PageResult;
import com.campus.runner.service.MessageService;
import com.campus.runner.service.RunnerAuditService;
import com.campus.runner.vo.RunnerAuditVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RunnerAuditServiceImpl implements RunnerAuditService {

    @Autowired
    private RunnerAuditMapper runnerAuditMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageService messageService;

    @Override
    public PageResult<RunnerAuditVO> page(Integer page, Integer pageSize, Integer status, String studentNo) {
        PageHelper.startPage(page, pageSize);
        Page<RunnerAudit> auditPage = (Page<RunnerAudit>) runnerAuditMapper.page(status, studentNo);
        List<RunnerAuditVO> vos = auditPage.getResult().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(auditPage.getTotal(), vos, auditPage.getPageSize(), auditPage.getPageNum());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(RunnerAuditProcessDTO dto) {
        RunnerAudit audit = runnerAuditMapper.getById(dto.getId());
        if (audit == null) {
            throw new BusinessException(MessageConstant.AUDIT_NOT_FOUND);
        }
        if (audit.getStatus() != RunnerConstant.REVIEW_PENDING) {
            throw new BusinessException(MessageConstant.AUDIT_ALREADY_PROCESSED);
        }
        //更新审核记录
        RunnerAudit upd = RunnerAudit.builder()
                .id(audit.getId())
                .status(dto.getStatus())
                .auditRemark(dto.getAuditRemark())
                .auditTime(LocalDateTime.now())
                .build();
        runnerAuditMapper.update(upd);
        //联动技能者认证状态
        int runnerAuditStatus = dto.getStatus() == RunnerConstant.REVIEW_PASSED
                ? RunnerConstant.AUDIT_PASSED : RunnerConstant.AUDIT_REJECTED;
        Runner runnerUpd = Runner.builder()
                .id(audit.getRunnerId())
                .auditStatus(runnerAuditStatus)
                .build();
        runnerMapper.update(runnerUpd);
        //站内消息：通知申请人审核结果（audit 表无 userId，经技能者记录解析）
        Runner notifiedRunner = runnerMapper.getById(audit.getRunnerId());
        if (notifiedRunner != null) {
            boolean passed = dto.getStatus() == RunnerConstant.REVIEW_PASSED;
            messageService.notify(notifiedRunner.getUserId(), passed ? "技能者认证已通过" : "技能者认证未通过",
                    (passed ? "恭喜！您的技能者认证已通过审核，现在可以登录技能者端接单了。"
                            : "很抱歉，您的技能者认证未通过审核。" + (dto.getAuditRemark() != null ? "原因：" + dto.getAuditRemark() : "")),
                    null);
        }
        log.info("认证审核完成，auditId={}, runnerId={}, result={}", dto.getId(), audit.getRunnerId(), dto.getStatus());
    }

    private RunnerAuditVO toVO(RunnerAudit audit) {
        RunnerAuditVO vo = RunnerAuditVO.builder()
                .id(audit.getId())
                .runnerId(audit.getRunnerId())
                .realName(audit.getRealName())
                .studentNo(audit.getStudentNo())
                .campus(audit.getCampus())
                .college(audit.getCollege())
                .idCard(audit.getIdCard())
                .studentCardImg(audit.getStudentCardImg())
                .status(audit.getStatus())
                .auditRemark(audit.getAuditRemark())
                .applyTime(audit.getApplyTime())
                .auditTime(audit.getAuditTime())
                .build();
        Runner runner = runnerMapper.getById(audit.getRunnerId());
        if (runner != null) {
            vo.setUserId(runner.getUserId());
        }
        return vo;
    }
}
