package com.yuxian.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class RefundFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联的订单ID
    private Long orderId;

    // 0: 用户申请, 1: 管理员反馈/拒绝
    private Integer type; 

    // 反馈内容 (用户申请原因 或 管理员审核意见/拒绝理由)
    @Column(length = 500)
    private String content;

    // 操作人 (用户 username 或 管理员 adminUsername)
    private String operator; 

    // 反馈时间
    private LocalDateTime createTime;

    // 构造函数：确保创建时间自动设置
    public RefundFeedback() {
        this.createTime = LocalDateTime.now();
    }

    // Getters and Setters (省略，假设你使用Lombok或手动添加)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}