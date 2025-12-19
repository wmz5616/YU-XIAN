package com.yuxian.backend.service;

import com.yuxian.backend.entity.Address;
import com.yuxian.backend.entity.OrderRecord;
import java.util.List;
import java.util.Map;

public interface OrderService {
    Long createOrder(String username, List<Map<String, Object>> itemPayloads, Address addressSnapshot, Long couponId);

    void applyRefund(Long orderId, String reason, String type, String username);

    List<OrderRecord> getPendingRefundOrders();

    void auditRefund(Long orderId, boolean pass, String rejectReason, String adminUsername);

    void payOrder(Long orderId, String username);
}