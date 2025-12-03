package com.yuxian.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users") // 数据库表名叫 users
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 用户名必须唯一，不能重复
    @Column(unique = true, nullable = false)
    private String username;

    // 密码 (实际项目中需要加密，这里为了教学简便，先存明文)
    @Column(nullable = false)
    private String password;

    // 昵称 (显示在页面右上角的名字)
    private String displayName;

    @Lob
@Column(columnDefinition = "CLOB") // H2/MySQL 兼容写法
private String avatar;
}