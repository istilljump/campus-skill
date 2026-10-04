package com.campus.runner.service.impl;

import com.campus.runner.constant.MessageConstant;
import com.campus.runner.dto.PortfolioAuditProcessDTO;
import com.campus.runner.dto.PortfolioDTO;
import com.campus.runner.entity.Portfolio;
import com.campus.runner.entity.Runner;
import com.campus.runner.entity.ServiceItem;
import com.campus.runner.exception.BusinessException;
import com.campus.runner.mapper.PortfolioMapper;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.mapper.ServiceItemMapper;
import com.campus.runner.result.PageResult;
import com.campus.runner.service.MessageService;
import com.campus.runner.service.PortfolioService;
import com.campus.runner.service.ServiceItemService;
import com.campus.runner.vo.ServiceItemVO;
import com.campus.runner.vo.SkillerProfileVO;
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
public class PortfolioServiceImpl implements PortfolioService {

    @Autowired
    private PortfolioMapper portfolioMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Autowired
    private ServiceItemMapper serviceItemMapper;

    @Autowired
    private ServiceItemService serviceItemService;

    @Autowired
    private MessageService messageService;

    @Override
    public List<Portfolio> listOwn(Long skillerId) {
        return portfolioMapper.listBySkiller(skillerId);
    }

    @Override
    public void create(Long skillerId, PortfolioDTO dto) {
        portfolioMapper.insert(Portfolio.builder()
                .skillerId(skillerId)
                .title(dto.getTitle())
                .categoryId(dto.getCategoryId())
                .coverUrl(dto.getCoverUrl())
                .workUrls(dto.getWorkUrls())
                .description(dto.getDescription())
                .status(Portfolio.PENDING)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());
        log.info("作品上传成功，skillerId={}, title={}", skillerId, dto.getTitle());
    }

    @Override
    public void delete(Long skillerId, Long id) {
        Portfolio portfolio = portfolioMapper.getById(id);
        if (portfolio == null || !portfolio.getSkillerId().equals(skillerId)) {
            throw new BusinessException("作品不存在");
        }
        portfolioMapper.deleteByIdAndSkiller(id, skillerId);
    }

    @Override
    public PageResult<Portfolio> page(Integer page, Integer pageSize, Integer status) {
        PageHelper.startPage(page, pageSize);
        Page<Portfolio> portfolioPage = (Page<Portfolio>) portfolioMapper.listPending(status);
        return new PageResult<>(portfolioPage.getTotal(), portfolioPage.getResult(),
                portfolioPage.getPageSize(), portfolioPage.getPageNum());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(PortfolioAuditProcessDTO dto, Long adminId) {
        Portfolio portfolio = portfolioMapper.getById(dto.getId());
        if (portfolio == null) {
            throw new BusinessException("作品不存在");
        }
        if (portfolio.getStatus() == null || portfolio.getStatus() != Portfolio.PENDING) {
            throw new BusinessException("该作品已审核，请勿重复处理");
        }
        portfolioMapper.updateStatus(dto.getId(), dto.getStatus(), dto.getAuditOpinion(), adminId);
        //审核通过且指定了作品定级：联动技能者 skill_level
        if (dto.getStatus() == Portfolio.PASSED && dto.getSkillLevel() != null
                && dto.getSkillLevel() >= 1 && dto.getSkillLevel() <= 3) {
            Runner runnerUpd = Runner.builder()
                    .id(portfolio.getSkillerId())
                    .skillLevel(dto.getSkillLevel())
                    .build();
            runnerMapper.update(runnerUpd);
        }
        //站内消息：通知技能者审核结果
        Runner skiller = runnerMapper.getById(portfolio.getSkillerId());
        if (skiller != null) {
            boolean passed = dto.getStatus() == Portfolio.PASSED;
            String levelText = passed && dto.getSkillLevel() != null ? "，作品定级：C" + dto.getSkillLevel() : "";
            messageService.notify(skiller.getUserId(), passed ? "作品审核通过" : "作品审核未通过",
                    (passed ? "恭喜！您的作品「" + portfolio.getTitle() + "」已通过审核" + levelText + "。"
                            : "很抱歉，您的作品「" + portfolio.getTitle() + "」未通过审核。")
                            + (dto.getAuditOpinion() != null ? "审核意见：" + dto.getAuditOpinion() : ""),
                    null);
        }
        log.info("作品审核完成，portfolioId={}, skillerId={}, status={}, skillLevel={}",
                dto.getId(), portfolio.getSkillerId(), dto.getStatus(), dto.getSkillLevel());
    }

    @Override
    public SkillerProfileVO getSkillerProfile(Long skillerId) {
        Runner skiller = runnerMapper.getById(skillerId);
        if (skiller == null) {
            throw new BusinessException(MessageConstant.RUNNER_NOT_FOUND);
        }
        //主页仅展示审核通过的作品与上架中的服务
        List<Portfolio> portfolio = portfolioMapper.listBySkiller(skillerId).stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == Portfolio.PASSED)
                .collect(Collectors.toList());
        List<ServiceItemVO> services = serviceItemMapper.listBySkiller(skillerId).stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == ServiceItem.ON_SHELF)
                .map(s -> serviceItemService.toVO(s))
                .collect(Collectors.toList());
        return SkillerProfileVO.builder()
                .skillerId(skiller.getId())
                .name(skiller.getName())
                .campus(skiller.getCampus())
                .college(skiller.getCollege())
                .score(skiller.getScore())
                .creditScore(skiller.getCreditScore())
                .skillLevel(skiller.getSkillLevel())
                .completedOrders(skiller.getCompletedOrders())
                .portfolio(portfolio)
                .services(services)
                .build();
    }
}
