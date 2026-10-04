package com.campus.runner.service.impl;

import com.campus.runner.constant.MessageConstant;
import com.campus.runner.constant.RunnerConstant;
import com.campus.runner.dto.ServiceItemDTO;
import com.campus.runner.entity.ErrandType;
import com.campus.runner.entity.Runner;
import com.campus.runner.entity.ServiceItem;
import com.campus.runner.exception.BusinessException;
import com.campus.runner.mapper.ErrandTypeMapper;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.mapper.ServiceItemMapper;
import com.campus.runner.result.PageResult;
import com.campus.runner.service.ServiceItemService;
import com.campus.runner.vo.ServiceItemVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ServiceItemServiceImpl implements ServiceItemService {

    //新服务默认值
    private static final int DEFAULT_DELIVERY_DAYS = 3;
    private static final int DEFAULT_SERVICE_MODE = 1;

    @Autowired
    private ServiceItemMapper serviceItemMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Autowired
    private ErrandTypeMapper errandTypeMapper;

    @Override
    public List<ServiceItem> listOwn(Long skillerId) {
        return serviceItemMapper.listBySkiller(skillerId);
    }

    @Override
    public Long create(Long skillerId, ServiceItemDTO dto) {
        //类目校验
        checkCategory(dto.getCategoryId());
        //仅认证通过的技能者可挂牌服务
        checkSkillerCertified(skillerId);
        ServiceItem item = ServiceItem.builder()
                .skillerId(skillerId)
                .categoryId(dto.getCategoryId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .deliveryDays(dto.getDeliveryDays() == null ? DEFAULT_DELIVERY_DAYS : dto.getDeliveryDays())
                .serviceMode(dto.getServiceMode() == null ? DEFAULT_SERVICE_MODE : dto.getServiceMode())
                .tags(dto.getTags())
                .status(ServiceItem.ON_SHELF)
                .salesCount(0)
                .avgScore(new BigDecimal("5.0"))
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        serviceItemMapper.insert(item);
        log.info("服务发布成功，skillerId={}, title={}, id={}", skillerId, dto.getTitle(), item.getId());
        return item.getId();
    }

    @Override
    public void update(Long skillerId, Long id, ServiceItemDTO dto) {
        ServiceItem item = getOwnedItem(skillerId, id);
        if (dto.getCategoryId() != null) {
            checkCategory(dto.getCategoryId());
        }
        ServiceItem upd = ServiceItem.builder()
                .id(item.getId())
                .categoryId(dto.getCategoryId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .deliveryDays(dto.getDeliveryDays())
                .serviceMode(dto.getServiceMode())
                .tags(dto.getTags())
                .build();
        serviceItemMapper.update(upd);
        log.info("服务更新成功，serviceItemId={}, skillerId={}", id, skillerId);
    }

    @Override
    public void updateStatus(Long skillerId, Long id, Integer status) {
        getOwnedItem(skillerId, id);
        serviceItemMapper.update(ServiceItem.builder().id(id).status(status).build());
        log.info("服务状态更新，serviceItemId={}, skillerId={}, status={}", id, skillerId, status);
    }

    @Override
    public PageResult<ServiceItemVO> pageOnShelf(Integer page, Integer pageSize, Long categoryId, String keyword) {
        PageHelper.startPage(page, pageSize);
        Page<ServiceItemVO> voPage = (Page<ServiceItemVO>) serviceItemMapper.listOnShelf(categoryId, keyword);
        return new PageResult<>(voPage.getTotal(), voPage.getResult(), voPage.getPageSize(), voPage.getPageNum());
    }

    @Override
    public ServiceItemVO getVOById(Long id) {
        ServiceItemVO vo = serviceItemMapper.getVOById(id);
        if (vo == null) {
            throw new BusinessException("服务不存在");
        }
        return vo;
    }

    @Override
    public ServiceItemVO toVO(ServiceItem item) {
        ServiceItemVO vo = ServiceItemVO.builder()
                .id(item.getId())
                .skillerId(item.getSkillerId())
                .categoryId(item.getCategoryId())
                .title(item.getTitle())
                .description(item.getDescription())
                .price(item.getPrice())
                .deliveryDays(item.getDeliveryDays())
                .serviceMode(item.getServiceMode())
                .tags(item.getTags())
                .status(item.getStatus())
                .salesCount(item.getSalesCount())
                .avgScore(item.getAvgScore())
                .createTime(item.getCreateTime())
                .updateTime(item.getUpdateTime())
                .build();
        Runner skiller = runnerMapper.getById(item.getSkillerId());
        if (skiller != null) {
            vo.setSkillerName(skiller.getName());
            vo.setSkillerScore(skiller.getScore());
            vo.setSkillerCreditScore(skiller.getCreditScore());
            vo.setSkillerSkillLevel(skiller.getSkillLevel());
        }
        if (item.getCategoryId() != null) {
            ErrandType category = errandTypeMapper.getById(item.getCategoryId());
            vo.setCategoryName(category != null ? category.getName() : null);
        }
        return vo;
    }

    private void checkCategory(Long categoryId) {
        ErrandType category = errandTypeMapper.getById(categoryId);
        if (category == null || category.getStatus() == null || category.getStatus() != 1) {
            throw new BusinessException("技能类目不存在或已停用");
        }
    }

    private void checkSkillerCertified(Long skillerId) {
        Runner skiller = runnerMapper.getById(skillerId);
        if (skiller == null) {
            throw new BusinessException(MessageConstant.RUNNER_NOT_FOUND);
        }
        if (skiller.getAuditStatus() == null || skiller.getAuditStatus() != RunnerConstant.AUDIT_PASSED) {
            throw new BusinessException("仅认证通过的技能者可发布服务");
        }
    }

    private ServiceItem getOwnedItem(Long skillerId, Long id) {
        ServiceItem item = serviceItemMapper.getById(id);
        if (item == null || !item.getSkillerId().equals(skillerId)) {
            throw new BusinessException("服务不存在");
        }
        return item;
    }
}
