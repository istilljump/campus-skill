package com.campus.runner.service;

import com.campus.runner.entity.Announcement;
import com.campus.runner.result.PageResult;

/**
 * 管理端-系统公告
 */
public interface AnnouncementService {

    /**
     * 发布系统公告：落库主记录 + 全量广播收件箱（用户端/技能者端自动可见）
     */
    void broadcast(Long adminId, String title, String content);

    /**
     * 公告历史分页
     */
    PageResult<Announcement> page(Integer page, Integer pageSize);
}
