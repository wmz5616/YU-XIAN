package com.yuxian.backend.controller;

import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin // 允许前端跨域访问
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 动作：注册
    // 地址：POST /api/users/register
    @PostMapping("/register")
    public String register(@RequestBody User user) {
        // 1. 检查用户名是否已被占用
        User existing = userRepository.findByUsername(user.getUsername());
        if (existing != null) {
            return "注册失败：用户名已存在";
        }
        
        // 2. 如果没被占用，就保存到数据库
        // 如果用户没填昵称，默认昵称就是用户名
        if (user.getDisplayName() == null) {
            user.setDisplayName(user.getUsername());
        }
        
        userRepository.save(user);
        return "注册成功";
    }

    // 动作：登录
    // 地址：POST /api/users/login
    @PostMapping("/login")
    public User login(@RequestBody User loginUser) {
        // 1. 根据用户名去数据库查
        User user = userRepository.findByUsername(loginUser.getUsername());
        
        // 2. 如果用户不存在，或者密码不对
        if (user == null || !user.getPassword().equals(loginUser.getPassword())) {
            // 返回 null 表示登录失败 (前端会根据是否为 null 判断)
            return null; 
        }
        
        // 3. 登录成功，返回用户完整信息
        return user;
    }

    @PostMapping("/avatar")
public User updateAvatar(@RequestBody Map<String, String> payload) {
    String username = payload.get("username");
    String avatarBase64 = payload.get("avatar");

    User user = userRepository.findByUsername(username);
    if (user != null) {
        user.setAvatar(avatarBase64);
        userRepository.save(user);
        return user; // 返回更新后的用户
    }
    return null;
}
}