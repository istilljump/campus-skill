package com.campus.runner.service.impl;

import com.campus.runner.entity.Dispute;
import com.campus.runner.entity.Orders;
import com.campus.runner.entity.User;
import com.campus.runner.mapper.DisputeMapper;
import com.campus.runner.mapper.OrderMapper;
import com.campus.runner.mapper.UserMapper;
import com.campus.runner.result.PageResult;
import com.campus.runner.service.DisputeService;
import com.campus.runner.vo.DisputeVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DisputeServiceImpl implements DisputeService {

    @Autowired
    private DisputeMapper disputeMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResult<DisputeVO> page(Integer page, Integer pageSize, Integer status) {
        PageHelper.startPage(page, pageSize);
        Page<Dispute> disputePage = (Page<Dispute>) disputeMapper.listByStatus(status);
        List<DisputeVO> vos = disputePage.getResult().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(disputePage.getTotal(), vos, disputePage.getPageSize(), disputePage.getPageNum());
    }

    private DisputeVO toVO(Dispute dispute) {
        DisputeVO vo = DisputeVO.builder()
                .id(dispute.getId())
                .orderId(dispute.getOrderId())
                .raisedBy(dispute.getRaisedBy())
                .reasonType(dispute.getReasonType())
                .description(dispute.getDescription())
                .evidenceUrls(dispute.getEvidenceUrls())
                .status(dispute.getStatus())
                .verdict(dispute.getVerdict())
                .adminId(dispute.getAdminId())
                .createTime(dispute.getCreateTime())
                .updateTime(dispute.getUpdateTime())
                .build();
        if (dispute.getOrderId() != null) {
            Orders order = orderMapper.getById(dispute.getOrderId());
            if (order != null) {
                vo.setOrderTitle(order.getTitle());
                vo.setOrderNumber(order.getNumber());
            }
        }
        if (dispute.getRaisedBy() != null) {
            User user = userMapper.getById(dispute.getRaisedBy());
            if (user != null) {
                vo.setUserName(user.getName());
            }
        }
        return vo;
    }
}
