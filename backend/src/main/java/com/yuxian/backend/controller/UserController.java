package com.yuxian.backend.controller;

import com.yuxian.backend.entity.Address;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.yuxian.backend.utils.JwtUtils;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();

        String regex = "^[a-z0-9]{1,7}$";
        if (!Pattern.matches(regex, user.getUsername())) {
            response.put("success", false);
            response.put("message", "注册失败：用户名必须是小写字母+数字，且不超过7位");
            return response;
        }

        if (userRepository.findByUsername(user.getUsername()) != null) {
            response.put("success", false);
            response.put("message", "注册失败：该用户名已被占用");
            return response;
        }

        if (user.getDisplayName() == null || user.getDisplayName().isEmpty()) {
            user.setDisplayName("会员" + user.getUsername());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        userRepository.save(user);
        response.put("success", true);
        response.put("message", "注册成功");
        return response;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginUser) {
        User user = userRepository.findByUsername(loginUser.getUsername());
        if (user == null || !passwordEncoder.matches(loginUser.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误");
        }
        String token = jwtUtils.generateToken(user.getUsername());
        
        Map<String, Object> response = new HashMap<>();
        response.put("user", user);     
        response.put("role", user.getRole());
        response.put("token", token);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/address")
    @Transactional
    public ResponseEntity<?> updateAddress(@RequestBody User userWithAddress) {
        User user = userRepository.findByUsername(userWithAddress.getUsername());
        if (user != null) {
            user.getAddresses().clear();
            if (userWithAddress.getAddresses() != null) {
                user.getAddresses().addAll(userWithAddress.getAddresses());
            }
            User savedUser = userRepository.save(user);
            return ResponseEntity.ok(savedUser);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户不存在，请重新登录");
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

    @GetMapping("/info")
    public User getUserInfo(@RequestParam String username) {
        return userRepository.findByUsername(username);
    }
}