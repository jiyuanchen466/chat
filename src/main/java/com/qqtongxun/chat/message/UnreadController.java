package com.qqtongxun.chat.message;

import com.qqtongxun.chat.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/unread")
public class UnreadController {

    private final UnreadService unreadService;

    public UnreadController(UnreadService unreadService) {
        this.unreadService = unreadService;
    }

    // 获取某人所有未读
    @GetMapping
    public Result<Map<String, Integer>> getUnread(@RequestParam String userId) {
        return Result.success(unreadService.getUnread(userId));
    }

    // 打开会话时，把某个好友的未读清零
    @PostMapping("/read")
    public Result<?> markRead(@RequestBody Map<String, String> req) {
        unreadService.clear(req.get("userId"), req.get("fromUserId"));
        return Result.success("已读", null);
    }
}