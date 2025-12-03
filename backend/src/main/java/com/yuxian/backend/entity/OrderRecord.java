package com.yuxian.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class OrderRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;      // 谁买的
    private String productNames;  // 买了啥 (存成字符串，如 "鲍鱼, 龙虾")
    private Double totalPrice;    // 总价
    private String status;        // 状态 (待发货/运输中/已送达)
    private LocalDateTime createTime; // 下单时间
}