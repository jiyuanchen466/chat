package com.qqtongxun.chat.user;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qqtongxun.chat.common.Result;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // 获取所有用户（作为好友列表，排除自己）
    @GetMapping
    public Result<List<User>> getAllUsers(@RequestParam(required = false) String currentUserId) {
        List<User> users = userRepository.findAll();
        
        // 过滤掉自己
        if (currentUserId != null && !currentUserId.isEmpty()) {
            users = users.stream()
                    .filter(u -> !u.getUserId().equals(currentUserId))
                    .collect(Collectors.toList());
        }
        
        // 出于隐私，清空密码字段再返回
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }
    // 更新个人资料
    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody Map<String, Object> req) {
        String userId = (String) req.get("userId");
        String nickname = (String) req.get("nickname");
        String avatar = (String) req.get("avatar");
        String signature = (String) req.get("signature");
        Integer gender = req.get("gender") != null ? Integer.parseInt(req.get("gender").toString()) : null;
        String email = (String) req.get("email");

        if (userId == null || userId.isEmpty()) {
            return Result.error("用户ID不能为空");
        }

        try {
            User user = userService.updateProfile(userId, nickname, avatar, signature, gender, email);
            user.setPassword(null); // 隐藏密码
            return Result.success("资料更新成功", user);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
    // 根据 userId 查询单个用户
    @GetMapping("/{userId}")
    public Result<User> getUserById(@PathVariable String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }
    @GetMapping("/search")
    public Result<List<User>> searchUsers(@RequestParam String keyword,
                                        @RequestParam String currentUserId) {
        List<User> users = userRepository.findByUsernameContainingOrNicknameContaining(keyword, keyword);
        users = users.stream()
                .filter(u -> !u.getUserId().equals(currentUserId))
                .collect(Collectors.toList());
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }
}