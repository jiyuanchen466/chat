package com.qqtongxun.chat.friend;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFriendRepository extends JpaRepository<UserFriend, Long> {

    // 我发出去的所有申请/好友关系
    List<UserFriend> findByUserIdAndStatus(String userId, Integer status);

    // 别人发给我的申请（我是接收人）
    List<UserFriend> findByFriendIdAndStatus(String friendId, Integer status);

    // 查两个人之间是否已有关系
    Optional<UserFriend> findByUserIdAndFriendId(String userId, String friendId);
}