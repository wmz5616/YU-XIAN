package com.yuxian.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "wallet_logs")
public class WalletLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer type;

    private String description;

    @Column(nullable = false)
    private LocalDateTime createTime;

    public WalletLog() {
        this.createTime = LocalDateTime.now();
    }
}
