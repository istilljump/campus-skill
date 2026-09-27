package com.campus.runner.websocket;

import com.campus.runner.constant.JwtClaimsConstant;
import com.campus.runner.properties.JwtProperties;
import com.campus.runner.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 实时推送服务端点：/ws/{token}
 * 浏览器 WebSocket 无法自定义请求头，故通过路径携带 JWT（用户端/跑腿员端用户 token 均含 userId 声明）
 * 同一用户允许多个页面连接，按 userId 分组管理
 */
@Component
@ServerEndpoint("/ws/{token}")
@Slf4j
public class WsServer {

    /** userId -> 该用户的全部在线连接 */
    private static final Map<Long, Set<Session>> SESSIONS = new ConcurrentHashMap<>();

    private static JwtProperties jwtProperties;

    @Autowired
    public void setJwtProperties(JwtProperties jwtProperties) {
        WsServer.jwtProperties = jwtProperties;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        Long userId = parseUserId(token);
        if (userId == null) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "token不合法"));
            } catch (IOException ignored) {
            }
            return;
        }
        session.getUserProperties().put("userId", userId);
        SESSIONS.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("WebSocket连接建立，userId={}，当前在线连接组数={}", userId, SESSIONS.size());
    }

    @OnClose
    public void onClose(Session session) {
        remove(session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        remove(session);
        log.debug("WebSocket连接异常断开: {}", error.getMessage());
    }

    private void remove(Session session) {
        Object userId = session.getUserProperties().get("userId");
        if (userId != null) {
            Set<Session> set = SESSIONS.get(userId);
            if (set != null) {
                set.remove(session);
                if (set.isEmpty()) {
                    SESSIONS.remove(userId);
                }
            }
        }
    }

    private Long parseUserId(String token) {
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            return claims.get(JwtClaimsConstant.USER_ID, Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 向指定用户的全部在线连接推送文本消息（离线则静默忽略，消息已落库）
     */
    public static void sendToUser(Long userId, String text) {
        Set<Session> set = SESSIONS.get(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        for (Session session : set) {
            if (session.isOpen()) {
                try {
                    session.getAsyncRemote().sendText(text);
                } catch (Exception e) {
                    log.debug("WebSocket推送失败，userId={}", userId, e);
                }
            }
        }
    }
}
