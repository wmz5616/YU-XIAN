package com.yuxian.backend.service;

import com.yuxian.backend.entity.Address;
import com.yuxian.backend.entity.OrderRecord;
import java.util.List;
import java.util.Map;

public interface OrderService {
    
    // 创建订单
    Long createOrder(String username, List<Map<String, Object>> itemPayloads, Address addressSnapshot, Long couponId);

    // 申请售后
    void applyRefund(Long orderId, String reason, String type, String username);

    // 获取待处理售后列表
    List<OrderRecord> getPendingRefundOrders();

    // 审核售后
    void auditRefund(Long orderId, boolean pass, String rejectReason, String adminUsername);

    // ✅ 新增：订单支付
    void payOrder(Long orderId, String username);
}