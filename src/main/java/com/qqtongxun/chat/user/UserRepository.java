package com.qqtongxun.chat.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // 根据 username 查用户
    Optional<User> findByUsername(String username);

    // 模糊搜索：用户名或昵称包含关键字
    List<User> findByUsernameContainingOrNicknameContaining(String username, String nickname);
}