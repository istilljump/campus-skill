package com.campus.runner.websocket;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 实时推送入口：业务侧调用，向在线用户推送事件（基于 WsServer 会话注册表）
 */
@Component
@Slf4j
public class WebSocketPusher {

    /**
     * 推送站内消息事件（与 MessageService.notify 对应）
     */
    public void pushMessage(Long userId, String title, String content, Long orderId) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("type", "message");
            payload.put("title", title);
            payload.put("content", content);
            payload.put("orderId", orderId);
            WsServer.sendToUser(userId, payload.toJSONString());
        } catch (Exception e) {
            //推送失败不影响主流程，消息已落库可事后查看
            log.debug("实时推送失败，userId={}, title={}", userId, title, e);
        }
    }
}
