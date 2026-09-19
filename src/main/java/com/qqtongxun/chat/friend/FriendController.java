package com.qqtongxun.chat.friend;

import com.qqtongxun.chat.common.Result;
import com.qqtongxun.chat.user.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    // 获取好友列表
    @GetMapping
    public Result<List<User>> getFriends(@RequestParam String userId) {
        return Result.success(friendService.getFriends(userId));
    }

    // 发送好友申请
    @PostMapping("/request")
    public Result<?> sendRequest(@RequestBody Map<String, String> req) {
        String fromUserId = req.get("fromUserId");
        String toUserId = req.get("toUserId");
        try {
            friendService.sendRequest(fromUserId, toUserId);
            return Result.success("申请已发送", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 同意申请
    @PostMapping("/accept")
    public Result<?> accept(@RequestBody Map<String, String> req) {
        try {
            friendService.acceptRequest(req.get("userId"), req.get("requesterId"));
            return Result.success("已同意", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 拒绝申请
    @PostMapping("/reject")
    public Result<?> reject(@RequestBody Map<String, String> req) {
        try {
            friendService.rejectRequest(req.get("userId"), req.get("requesterId"));
            return Result.success("已拒绝", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 获取待处理好友申请
    @GetMapping("/requests")
    public Result<List<Map<String, Object>>> getRequests(@RequestParam String userId) {
        return Result.success(friendService.getPendingRequests(userId));
    }
}