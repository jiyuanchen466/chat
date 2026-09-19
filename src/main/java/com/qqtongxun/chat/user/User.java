package com.qqtongxun.chat.user;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Entity
@Table(name = "user")
public class User {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    // 后端双重保险：只允许英文、数字、下划线，4-20位
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名只能包含英文、数字和下划线，长度为4-20位")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @Column(name = "signature", length = 200)
    private String signature;

    @Column(name = "gender")
    private Integer gender;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "status")
    private Integer status;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    // 在保存到数据库之前自动执行
    @PrePersist
    public void prePersist() {
        if (this.userId == null) {
            this.userId = UUID.randomUUID().toString();
        }
        if (this.createTime == null) {
            this.createTime = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = 1; // 1正常
        }
        if (this.gender == null) {
            this.gender = 0; // 0未知
        }
        if (this.nickname == null || this.nickname.trim().isEmpty()) {
            this.nickname = this.username; // 默认昵称等于用户名
        }
        if (this.avatar == null) {
            // 默认随机头像，使用 DiceBear 开源 API
            this.avatar = "https://api.dicebear.com/7.x/avataaars/svg?seed=" + this.username;
        }
    }
}