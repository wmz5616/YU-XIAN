package com.yuxian.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundDetailVO {
    private Long orderId;
    private String username;
    private BigDecimal amount;
    private String reason;
    private String status;
    private String productNames;
    private LocalDateTime applyTime;

    // 构造函数
    public RefundDetailVO(Long orderId, String username, BigDecimal amount, String reason, String status, String productNames, LocalDateTime applyTime) {
        this.orderId = orderId;
        this.username = username;
        this.amount = amount;
        this.reason = reason;
        this.status = status;
        this.productNames = productNames;
        this.applyTime = applyTime;
    }

    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProductNames() { return productNames; }
    public void setProductNames(String productNames) { this.productNames = productNames; }
    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }
}