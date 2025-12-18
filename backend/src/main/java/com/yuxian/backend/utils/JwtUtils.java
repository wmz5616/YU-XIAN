package com.yuxian.backend.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value; // ✅ 新增导入
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct; // ✅ 新增导入 (如果是 Spring Boot 2.x 请用 javax.annotation.PostConstruct)
import java.security.Key;
import java.util.Date;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtils {

    // ❌ 删除原有的 static final 常量
    // private static final String SECRET_STRING = "...";
    // private static final Key key = ...;

    // ✅ 新增：从配置文件注入密钥
    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.expiration:86400000}") // 默认 1 天
    private long expirationTime;

    private Key key; // 改为实例变量

    // ✅ 新增：初始化 Key
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // 使用注入的过期时间
                .signWith(key)
                .compact();
    }

    public String validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}