package com.yuxian.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // 优惠券名称，如 "新人专享券"
    private Double amount; // 减免金额，如 20.0
    private Double minSpend; // 最低消费门槛，如 100.0 (0代表无门槛)
    
    private Integer totalCount; // 总发行量
    private Integer receivedCount; // 已领取数量
    
    private LocalDate validUntil; // 有效期截止
    
    // 状态: 1=正常, 0=下架
    private Integer status; 
}