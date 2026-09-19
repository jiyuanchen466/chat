package com.qqtongxun.chat.websocket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.qqtongxun.chat.message.ChatMessage;
import com.qqtongxun.chat.message.ChatMessageRepository;
import com.qqtongxun.chat.message.UnreadService;

import tools.jackson.databind.ObjectMapper;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 在线用户：userId -> WebSocketSession
    private static final Map<String, WebSocketSession> ONLINE_SESSIONS = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatMessageRepository chatMessageRepository;
    private final UnreadService unreadService;

    public ChatWebSocketHandler(ChatMessageRepository chatMessageRepository, UnreadService unreadService) {
        this.chatMessageRepository = chatMessageRepository;
        this.unreadService = unreadService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            ONLINE_SESSIONS.put(userId, session);
            System.out.println("✅ 用户上线: " + userId + "，当前在线人数: " + ONLINE_SESSIONS.size());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            Map<String, String> msgMap = objectMapper.readValue(payload, Map.class);

            String fromUserId = msgMap.get("fromUserId");
            String toUserId = msgMap.get("toUserId");
            String content = msgMap.get("content");
            String type = msgMap.getOrDefault("type", "1");
            String sessionId = msgMap.getOrDefault("sessionId", "");

            System.out.println("📤 收到消息 from=" + fromUserId + " to=" + toUserId + " content=" + content);

            // 1. 保存消息到数据库
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(sessionId);
            chatMessage.setFromUserId(fromUserId);
            chatMessage.setContent(content);
            chatMessage.setType(Integer.parseInt(type));
            chatMessageRepository.save(chatMessage);

            // 2. 构造转发用的 Map（带上 toUserId，前端要靠它判断方向）
            Map<String, Object> forward = new HashMap<>();
            forward.put("messageId", chatMessage.getMessageId());
            forward.put("sessionId", sessionId);
            forward.put("fromUserId", fromUserId);
            forward.put("toUserId", toUserId);
            forward.put("content", content);
            forward.put("type", type);
            forward.put("createTime", chatMessage.getCreateTime() != null
                    ? chatMessage.getCreateTime().toString() : "");

            String newPayload = objectMapper.writeValueAsString(forward);

            // 3. 单聊转发
            if (toUserId != null && !toUserId.isEmpty()) {
                WebSocketSession targetSession = ONLINE_SESSIONS.get(toUserId);

                if (targetSession != null && targetSession.isOpen()) {
                    // 对方在线：推给他
                    targetSession.sendMessage(new TextMessage(newPayload));
                    System.out.println("✅ 消息已推送给在线用户: " + toUserId);
                } else {
                    System.out.println("⚠️ 目标用户不在线: " + toUserId + "，仅存库");
                }

                // ★ 给对方未读 +1（不管在不在线都加，上线后能看到红点）
                unreadService.increment(toUserId, fromUserId);

                // ⚠️ 注意：这里不再回发给发送者（前端已本地渲染，避免重复）
            } else {
                // 群聊广播（暂未启用）
                for (WebSocketSession s : ONLINE_SESSIONS.values()) {
                    if (s.isOpen()) {
                        s.sendMessage(new TextMessage(newPayload));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 处理消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            ONLINE_SESSIONS.remove(userId);
            System.out.println("❌ 用户下线: " + userId + "，当前在线人数: " + ONLINE_SESSIONS.size());
        }
    }

    private String getUserIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            return query.split("userId=")[1].split("&")[0];
        }
        return null;
    }
}