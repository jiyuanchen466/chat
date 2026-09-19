package com.qqtongxun.chat.config; 

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. 关闭 CSRF
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. 配置请求的放行规则
            .authorizeHttpRequests(auth -> auth
            // 放行静态资源
            .requestMatchers("/*.html", "/*.css", "/*.js", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
            // 放行登录、注册接口
            .requestMatchers("/api/auth/**").permitAll()
            // ★ 放行用户接口（新增）
            .requestMatchers("/api/users/**").permitAll()
            // 放行好友接口
            .requestMatchers("/api/friends/**").permitAll()
            // 放行文件上传
            .requestMatchers("/api/files/**").permitAll()
            // 放行 WebSocket
            .requestMatchers("/ws/**").permitAll()
            .requestMatchers("/api/messages/**", "/api/unread/**").permitAll()
            // 其他请求需要认证
            .anyRequest().authenticated()
        )
            
            // 3. 禁用自带表单登录页
            .formLogin(AbstractHttpConfigurer::disable)
            
            // 4. 禁用 HTTP Basic 弹窗
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    // 提前把密码加密器注入到 Spring 容器中
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}