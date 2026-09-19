package com.qqtongxun.chat.auth;

import com.qqtongxun.chat.common.Result;
import com.qqtongxun.chat.user.User;
import com.qqtongxun.chat.user.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // 注册接口
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String nickname = request.get("nickname");
        String password = request.get("password");

        try {
            userService.register(username, nickname, password);
            return Result.success("注册成功", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 登录接口
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        try {
            User user = userService.login(username, password);
            
            // 生成简单的 Token（后续可以换成 JWT 或存入 Redis）
            String token = java.util.UUID.randomUUID().toString().replace("-", "");
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", user.getUserId());
            data.put("nickname", user.getNickname());
            data.put("avatar", user.getAvatar());

            return Result.success("登录成功", data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}