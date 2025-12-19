package com.yuxian.backend.controller;

import com.yuxian.backend.entity.Address;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
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

        String regex = "^[a-zA-Z0-9]{4,20}$";
        if (!Pattern.matches(regex, user.getUsername())) {
            response.put("success", false);
            response.put("message", "注册失败：用户名需为4-20位字母或数字");
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

    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("username") String username,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件不能为空");
        }

        try {
            String projectPath = System.getProperty("user.dir");

            String staticPath = "/src/main/resources/static/images/avatars/";
            if (projectPath.endsWith("backend")) {
                staticPath = "/src/main/resources/static/images/avatars/";
            } else {
                staticPath = "/backend/src/main/resources/static/images/avatars/";
            }

            String uploadDir = projectPath + staticPath;
            File dir = new File(uploadDir);
            if (!dir.exists())
                dir.mkdirs();

            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String fileName = UUID.randomUUID().toString() + suffix;

            File dest = new File(dir, fileName);
            file.transferTo(dest);

            String avatarUrl = "/images/avatars/" + fileName;

            User user = userRepository.findByUsername(username);
            if (user != null) {
                user.setAvatar(avatarUrl);
                userRepository.save(user);
                return ResponseEntity.ok(user);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户不存在");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/info")
    public User getUserInfo(@RequestParam String username) {
        return userRepository.findByUsername(username);
    }
}