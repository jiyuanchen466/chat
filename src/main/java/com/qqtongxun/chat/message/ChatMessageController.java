package com.qqtongxun.chat.message;

import com.qqtongxun.chat.common.Result;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class ChatMessageController {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageController(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    // 拉取历史消息
    @GetMapping("/history")
    public Result<List<ChatMessage>> history(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createTime").descending());
        Page<ChatMessage> result = chatMessageRepository.findBySessionIdOrderByCreateTimeDesc(sessionId, pageable);

        // ★ 关键：用可修改的 ArrayList 包一层再反转
        List<ChatMessage> list = new java.util.ArrayList<>(result.getContent());
        java.util.Collections.reverse(list);

        return Result.success(list);
    }
}