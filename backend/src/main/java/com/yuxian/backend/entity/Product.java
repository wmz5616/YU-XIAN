package com.yuxian.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDate;

@Data // Lombok 注解，自动帮我们生成 get/set 方法
@Entity // 告诉 Spring 这是一个要存入数据库的实体
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 商品唯一编号

    // 对应数据：[贝类]
    private String category;

    // 对应数据：[鲍鱼]
    private String name;

    // 对应数据：[山东/大连]
    private String origin;

    // 对应数据：[2025.12.01]
    private LocalDate listDate;

    // 对应数据：[珍贵海味，多为人工养殖。]
    private String description;
    private Double price;
    private Integer stock;
    private String imageUrl; // 商品图片链接
}