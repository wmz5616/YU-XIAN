package com.yuxian.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_coupons")
public class UserCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username; // 领取人
    private Long couponId;   // 关联的优惠券ID
    
    // 冗余存储关键信息，防止优惠券被修改后产生纠纷
    private String couponName;
    private Double amount;
    private Double minSpend;
    private LocalDateTime receiveTime; // 领取时间
    
    // 状态: UNUSED(未使用), USED(已使用), EXPIRED(已过期)
    private String status;
}