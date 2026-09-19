package com.qqtongxun.chat.friend;

import com.qqtongxun.chat.user.User;
import com.qqtongxun.chat.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private final UserFriendRepository userFriendRepository;
    private final UserRepository userRepository;

    public FriendService(UserFriendRepository userFriendRepository, UserRepository userRepository) {
        this.userFriendRepository = userFriendRepository;
        this.userRepository = userRepository;
    }

    // 发送好友申请
    @Transactional
    public void sendRequest(String fromUserId, String toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能添加自己为好友");
        }
        // 检查是否已有关系
        if (userFriendRepository.findByUserIdAndFriendId(fromUserId, toUserId).isPresent()) {
            throw new RuntimeException("已经申请过或已是好友");
        }
        // 检查对方是否已申请我（双向）
        if (userFriendRepository.findByUserIdAndFriendId(toUserId, fromUserId).isPresent()) {
            throw new RuntimeException("对方已向你发送申请，请到'新的朋友'处理");
        }

        UserFriend uf = new UserFriend();
        uf.setUserId(fromUserId);
        uf.setFriendId(toUserId);
        uf.setStatus(0); // 申请中
        userFriendRepository.save(uf);
    }

    // 同意好友申请
    @Transactional
    public void acceptRequest(String myUserId, String requesterId) {
        UserFriend request = userFriendRepository.findByUserIdAndFriendId(requesterId, myUserId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));
        if (request.getStatus() != 0) {
            throw new RuntimeException("该申请已处理");
        }

        // 更新为已同意
        request.setStatus(1);
        userFriendRepository.save(request);

        // 反向插入，双方都能看到对方是好友
        UserFriend reverse = new UserFriend();
        reverse.setUserId(myUserId);
        reverse.setFriendId(requesterId);
        reverse.setStatus(1);
        userFriendRepository.save(reverse);
    }

    // 拒绝申请
    @Transactional
    public void rejectRequest(String myUserId, String requesterId) {
        UserFriend request = userFriendRepository.findByUserIdAndFriendId(requesterId, myUserId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));
        request.setStatus(2);
        userFriendRepository.save(request);
    }

    // 获取好友列表（返回 User 对象）
    public List<User> getFriends(String userId) {
        List<String> friendIds = userFriendRepository.findByUserIdAndStatus(userId, 1)
                .stream().map(UserFriend::getFriendId).collect(Collectors.toList());
        if (friendIds.isEmpty()) return Collections.emptyList();

        List<User> users = userRepository.findAllById(friendIds);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    // 获取待处理好友申请（返回申请人信息+申请ID）
    public List<Map<String, Object>> getPendingRequests(String userId) {
        List<UserFriend> requests = userFriendRepository.findByFriendIdAndStatus(userId, 0);
        if (requests.isEmpty()) return Collections.emptyList();

        List<String> requesterIds = requests.stream()
                .map(UserFriend::getUserId).collect(Collectors.toList());
        Map<String, User> userMap = userRepository.findAllById(requesterIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserFriend req : requests) {
            User u = userMap.get(req.getUserId());
            if (u == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("requestId", req.getId());
            item.put("userId", u.getUserId());
            item.put("username", u.getUsername());
            item.put("nickname", u.getNickname());
            item.put("avatar", u.getAvatar());
            item.put("createTime", req.getCreateTime());
            result.add(item);
        }
        return result;
    }
}