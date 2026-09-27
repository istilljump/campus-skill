package com.campus.runner.service.impl;

import com.campus.runner.entity.Message;
import com.campus.runner.mapper.MessageMapper;
import com.campus.runner.service.MessageService;
import com.campus.runner.websocket.WebSocketPusher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    private static final int LATEST_LIMIT = 20;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private WebSocketPusher webSocketPusher;

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
