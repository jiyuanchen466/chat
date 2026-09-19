package com.qqtongxun.chat.message;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UnreadService {

    // userId -> (对方 userId -> 未读数)
    private final Map<String, Map<String, Integer>> unreadMap = new ConcurrentHashMap<>();

    // 收到新消息时，给接收方未读 +1
    public void increment(String userId, String fromUserId) {
        unreadMap.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .merge(fromUserId, 1, Integer::sum);
    }

    // 查询某人的所有未读
    public Map<String, Integer> getUnread(String userId) {
        return unreadMap.getOrDefault(userId, Collections.emptyMap());
    }

    // 打开会话时清零
    public void clear(String userId, String fromUserId) {
        Map<String, Integer> map = unreadMap.get(userId);
        if (map != null) map.remove(fromUserId);
    }
}