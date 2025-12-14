package com.yuxian.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
public class OrderRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items;

    private Double totalPrice;
    private String status;
    private LocalDateTime createTime;

    private String productNames;

    public Double getTotalAmount() {
        return this.totalPrice; // 假设数据库字段叫 totalPrice
    }

    public void setStatus(String status) {
        this.status = status;
    }
}