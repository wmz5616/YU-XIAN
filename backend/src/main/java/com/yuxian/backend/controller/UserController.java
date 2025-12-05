package com.yuxian.backend.controller;

import com.yuxian.backend.entity.Address;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public String register(@RequestBody User user) {
    
        String regex = "^[a-z0-9]{1,7}$";
        if (!Pattern.matches(regex, user.getUsername())) {
            return "注册失败：用户名必须是小写字母+数字，且不超过7位";
        }

        if (userRepository.findByUsername(user.getUsername()) != null) {
            return "注册失败：该用户名已被占用";
        }

        if (user.getDisplayName() == null || user.getDisplayName().isEmpty()) {
            user.setDisplayName("会员" + user.getUsername());
        }
        
        userRepository.save(user);
        return "注册成功";
    }

    @PostMapping("/login")
    public User login(@RequestBody User loginUser) {
        User user = userRepository.findByUsername(loginUser.getUsername());
        if (user == null || !user.getPassword().equals(loginUser.getPassword())) {
            return null;
        }
        return user;
    }

    @PostMapping("/address")
    public User updateAddress(@RequestBody User userWithAddress) {
        User user = userRepository.findByUsername(userWithAddress.getUsername());
        if (user != null) {
            user.getAddresses().clear();
            if (userWithAddress.getAddresses() != null) {
                user.getAddresses().addAll(userWithAddress.getAddresses());
            }
            return userRepository.save(user);
        }
        return null;
    }

    @PostMapping("/avatar")
    public User updateAvatar(@RequestBody Map<String, String> payload) {
    String username = payload.get("username");
    String avatarBase64 = payload.get("avatar");

    User user = userRepository.findByUsername(username);
    if (user != null) {
        user.setAvatar(avatarBase64);
        userRepository.save(user);
        return user;
    }
    return null;
}
}