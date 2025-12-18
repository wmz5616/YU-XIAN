package com.yuxian.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "user_coupons")
public class UserCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private Long couponId;

    private String couponName;
    private BigDecimal amount;
    private BigDecimal minSpend;
    private LocalDateTime receiveTime;

    private String status;

    @Version
    private Integer version;
}