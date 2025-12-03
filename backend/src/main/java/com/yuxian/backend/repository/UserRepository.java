package com.yuxian.backend.repository;

import com.yuxian.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // 核心功能：根据用户名查找用户
    // (用于登录时检查账号是否存在)
    User findByUsername(String username);
}