package com.yuxian.backend.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtils {
    private static final String SECRET_STRING = "YuxianSeafoodSupplyChainSecretKey2025DoNotShare";
    private static final Key key = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));
    
    private static final long EXPIRATION_TIME = 86400000;
    
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
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