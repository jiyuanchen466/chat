package com.qqtongxun.chat.user;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 构造器注入
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 注册逻辑
    public User register(String username, String nickname, String password) {
        // 1. 检查用户名是否已存在
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("该用户名已被注册");
        }

        // 2. 构建用户对象
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        // 3. 密码加密（BCrypt）
        user.setPassword(passwordEncoder.encode(password));

        // 4. 保存到数据库（会自动触发 @PrePersist 生成 UUID、时间、默认头像）
        return userRepository.save(user);
    }

    // 登录逻辑
    public User login(String username, String password) {
        // 1. 根据用户名查询用户
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("用户名或密码错误");
        }

        User user = userOptional.get();

        // 2. 比对加密后的密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 检查账号是否被禁用
        if (user.getStatus() != 1) {
            throw new RuntimeException("该账号已被禁用，请联系管理员");
        }

        return user;
    }
    // 更新个人资料
    public User updateProfile(String userId, String nickname, String avatar, String signature, Integer gender, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (nickname != null && !nickname.trim().isEmpty()) user.setNickname(nickname.trim());
        if (avatar != null && !avatar.trim().isEmpty()) user.setAvatar(avatar.trim());
        if (signature != null) user.setSignature(signature);
        if (gender != null) user.setGender(gender);
        if (email != null) user.setEmail(email);

        return userRepository.save(user);
    }
}