package com.yuxian.backend.service;

import com.yuxian.backend.entity.Address;
import com.yuxian.backend.entity.OrderRecord;

import java.util.List;
import java.util.Map;

public interface OrderService {
    // ✅ 修改：增加了 Long couponId 参数
    void createOrder(String username, List<Map<String, Object>> itemPayloads, Address addressSnapshot, Long couponId);

    void applyRefund(Long orderId, String reason, String type, String username);

    // 获取所有待处理的售后订单
    List<OrderRecord> getPendingRefundOrders();

    // 审核售后 (pass=true 通过, pass=false 驳回)
    void auditRefund(Long orderId, boolean pass, String rejectReason, String adminUsername);
}