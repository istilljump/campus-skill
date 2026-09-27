package com.campus.runner.service;

import com.campus.runner.entity.Message;

import java.util.List;

public interface MessageService {

    /**
     * 发送站内消息（业务事件挂钩点调用，内部吞掉异常避免影响主流程）
     */
    void notify(Long userId, String title, String content, Long orderId);

    /**
     * 收件箱最新消息（默认20条）
     */
    List<Message> listLatest(Long userId, int limit);

    /**
     * 未读数量
     */
    int countUnread(Long userId);

    /**
     * 全部标记已读
     */
    void markAllRead(Long userId);
}
