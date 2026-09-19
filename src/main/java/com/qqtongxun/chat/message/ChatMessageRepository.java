package com.qqtongxun.chat.message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 按会话ID分页拉取历史消息，按时间倒序
    Page<ChatMessage> findBySessionIdOrderByCreateTimeDesc(String sessionId, Pageable pageable);
}