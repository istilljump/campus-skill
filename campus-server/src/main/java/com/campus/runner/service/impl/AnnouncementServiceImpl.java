package com.campus.runner.service.impl;

import com.campus.runner.entity.Announcement;
import com.campus.runner.entity.Employee;
import com.campus.runner.mapper.AnnouncementMapper;
import com.campus.runner.mapper.EmployeeMapper;
import com.campus.runner.result.PageResult;
import com.campus.runner.service.AnnouncementService;
import com.campus.runner.service.MessageService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Override
    public void broadcast(Long adminId, String title, String content) {
        //发布人姓名（查不到时降级为“管理员”）
        String adminName = "管理员";
        if (adminId != null) {
            try {
                Employee employee = employeeMapper.getById(adminId);
                if (employee != null && employee.getName() != null) {
                    adminName = employee.getName();
                }
            } catch (Exception ignored) {
            }
        }
        int recipientCount = messageService.broadcast(title, content);
        announcementMapper.insert(Announcement.builder()
                .title(title)
                .content(content)
                .recipientCount(recipientCount)
                .adminId(adminId)
                .adminName(adminName)
                .createTime(LocalDateTime.now())
                .build());
        log.info("系统公告已发布，title={}, 送达{}人，发布人={}", title, recipientCount, adminName);
    }

    @Override
    public PageResult<Announcement> page(Integer page, Integer pageSize) {
        PageHelper.startPage(page, pageSize);
        Page<Announcement> voPage = (Page<Announcement>) announcementMapper.listLatest();
        return new PageResult<>(voPage.getTotal(), voPage.getResult(), voPage.getPageSize(), voPage.getPageNum());
    }
}
