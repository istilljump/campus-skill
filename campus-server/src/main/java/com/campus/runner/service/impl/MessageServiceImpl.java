package com.campus.runner.service.impl;

import com.campus.runner.entity.Message;
import com.campus.runner.mapper.MessageMapper;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.mapper.UserMapper;
import com.campus.runner.service.MessageService;
import com.campus.runner.websocket.WebSocketPusher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    private static final int LATEST_LIMIT = 20;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private WebSocketPusher webSocketPusher;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Override
    public void notify(Long userId, String title, String content, Long orderId) {
        if (userId == null) {
            return;
        }
        try {
            messageMapper.insert(Message.builder()
                    .recipientId(userId)
                    .title(title)
                    .content(content)
                    .orderId(orderId)
                    .createTime(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            //消息发送失败不影响业务主流程
            log.warn("站内消息发送失败，userId={}, title={}", userId, title, e);
        }
        //在线用户实时推送（WebSocket）
        webSocketPusher.pushMessage(userId, title, content, orderId);
    }

    @Override
    public int broadcast(String title, String content) {
        //收件人 = 全体用户 ∪ 全体技能者（按用户id去重）
        Set<Long> recipientIds = new LinkedHashSet<>(userMapper.listAllIds());
        recipientIds.addAll(runnerMapper.listUserIds());
        recipientIds.remove(null);
        if (recipientIds.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Message> batch = new ArrayList<>(recipientIds.size());
        for (Long userId : recipientIds) {
            batch.add(Message.builder()
                    .recipientId(userId)
                    .title(title)
                    .content(content)
                    .createTime(now)
                    .build());
        }
        try {
            messageMapper.insertBatch(batch);
        } catch (Exception e) {
            log.warn("公告批量写入收件箱失败，title={}, recipients={}", title, recipientIds.size(), e);
            throw e;
        }
        //在线用户实时推送（WebSocket），推送失败不影响落库结果
        for (Long userId : recipientIds) {
            webSocketPusher.pushMessage(userId, title, content, null);
        }
        return recipientIds.size();
    }

    @Override
    public List<Message> listLatest(Long userId, int limit) {
        return messageMapper.listLatest(userId, Math.min(Math.max(limit, 1), LATEST_LIMIT));
    }

    @Override
    public int countUnread(Long userId) {
        return messageMapper.countUnread(userId);
    }

    @Override
    public void markAllRead(Long userId) {
        messageMapper.markAllRead(userId);
    }
}
