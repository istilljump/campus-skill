package com.campus.runner.service.impl;

import com.campus.runner.constant.MessageConstant;
import com.campus.runner.constant.StatusConstant;
import com.campus.runner.dto.ErrandTypeDTO;
import com.campus.runner.entity.ErrandType;
import com.campus.runner.exception.BusinessException;
import com.campus.runner.mapper.ErrandTypeMapper;
import com.campus.runner.mapper.OrderMapper;
import com.campus.runner.service.ErrandTypeService;
import com.campus.runner.vo.ErrandTypeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ErrandTypeServiceImpl implements ErrandTypeService {

    @Autowired
    private ErrandTypeMapper errandTypeMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public List<ErrandTypeVO> listEnabled() {
        return errandTypeMapper.listEnabled().stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ErrandTypeVO> listAll() {
        return errandTypeMapper.listAll().stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void save(ErrandTypeDTO dto) {
        ErrandType type = new ErrandType();
        BeanUtils.copyProperties(dto, type);
        if (type.getFeeRate() == null) {
            type.setFeeRate(new BigDecimal("0.10"));
        }
        if (type.getSort() == null) {
            type.setSort(0);
        }
        type.setStatus(StatusConstant.ENABLE);
        type.setCreateTime(LocalDateTime.now());
        type.setUpdateTime(LocalDateTime.now());
        errandTypeMapper.insert(type);
    }

    @Override
    public void update(ErrandTypeDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("类型id不能为空");
        }
        ErrandType exist = errandTypeMapper.getById(dto.getId());
        if (exist == null) {
            throw new BusinessException("订单类型不存在");
        }
        ErrandType type = new ErrandType();
        BeanUtils.copyProperties(dto, type);
        type.setUpdateTime(LocalDateTime.now());
        errandTypeMapper.update(type);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        ErrandType exist = errandTypeMapper.getById(id);
        if (exist == null) {
            throw new BusinessException("订单类型不存在");
        }
        errandTypeMapper.updateStatus(id, status);
    }

    @Override
    public void delete(Long id) {
        ErrandType exist = errandTypeMapper.getById(id);
        if (exist == null) {
            throw new BusinessException("订单类型不存在");
        }
        //存在关联订单时禁止删除，提示改用停用
        int related = orderMapper.countByTypeId(id);
        if (related > 0) {
            throw new BusinessException("该类型已存在关联订单，请使用停用功能");
        }
        errandTypeMapper.updateStatus(id, StatusConstant.DISABLE);
        log.info("订单类型已停用删除，id={}", id);
    }

    private ErrandTypeVO toVO(ErrandType type) {
        return ErrandTypeVO.builder()
                .id(type.getId())
                .name(type.getName())
                .icon(type.getIcon())
                .description(type.getDescription())
                .feeRate(type.getFeeRate())
                .build();
    }
}
