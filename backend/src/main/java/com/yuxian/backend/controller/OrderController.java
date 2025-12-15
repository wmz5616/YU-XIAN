package com.yuxian.backend.controller;

import com.yuxian.backend.entity.Address;
import com.yuxian.backend.entity.OrderRecord;
import com.yuxian.backend.service.OrderService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 下单接口
    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("username");
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

        // 解析地址
        Map<String, String> addrMap = (Map<String, String>) payload.get("address");
        Address address = new Address();
        address.setContact(addrMap.get("name"));
        address.setPhone(addrMap.get("phone"));
        address.setDetail(addrMap.get("detail"));

        // ✅ 新增：解析优惠券 ID
        Long couponId = null;
        if (payload.get("couponId") != null) {
            couponId = Long.valueOf(payload.get("couponId").toString());
        }

        // ✅ 传入 couponId
        orderService.createOrder(username, items, address, couponId);

        return ResponseEntity.ok("订单创建成功");
    }

    // ✅ 新增：处理售后申请
    @PostMapping("/{id}/refund")
    public ResponseEntity<String> applyRefund(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String reason = payload.get("reason");
        String type = payload.get("type");

        orderService.applyRefund(id, reason, type);

        return ResponseEntity.ok("售后申请已提交");
    }

    @GetMapping("/admin/refunds")
    public List<OrderRecord> getRefundOrders() {
        return orderService.getPendingRefundOrders();
    }

    @PostMapping("/admin/refunds/{id}/audit")
    public ResponseEntity<String> auditRefund(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        boolean pass = (boolean) payload.get("pass");
        String reason = (String) payload.get("reason");
        orderService.auditRefund(id, pass, reason);
        return ResponseEntity.ok(pass ? "已同意退款" : "已驳回申请");
    }
}