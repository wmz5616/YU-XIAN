package com.yuxian.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Data
@Entity
public class OrderRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items;

    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createTime;

    private String productNames;

    public BigDecimal getTotalAmount() {
        return this.totalPrice;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
}