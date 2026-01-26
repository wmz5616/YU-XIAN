package com.yuxian.backend.controller;

import com.yuxian.backend.entity.Address;
import com.yuxian.backend.service.OrderService;
import com.yuxian.backend.entity.OrderRecord;
import com.yuxian.backend.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public ResponseEntity<List<OrderRecord>> getMyOrders() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<OrderRecord> orders = orderRepository.findByUsernameOrderByCreateTimeDesc(username);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> payload) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

        Map<String, String> addrMap = (Map<String, String>) payload.get("address");
        Address address = new Address();
        address.setContact(addrMap.get("contact"));
        address.setPhone(addrMap.get("phone"));
        address.setDetail(addrMap.get("detail"));

        Long couponId = null;
        if (payload.get("couponId") != null) {
            couponId = Long.valueOf(payload.get("couponId").toString());
        }

        Long orderId = orderService.createOrder(username, items, address, couponId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "订单已创建，请前往支付",
                "orderId", orderId));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<String> applyRefund(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String reason = payload.get("reason");
        String type = payload.get("type");

        orderService.applyRefund(id, reason, type, username);
        return ResponseEntity.ok("售后申请已提交，等待审核");
    }

    @GetMapping("/admin/refunds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingRefunds() {
        return ResponseEntity.ok(orderService.getPendingRefundsWithDetails());
    }

    @PostMapping("/admin/refunds/{id}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> auditRefund(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        Boolean passObj = (Boolean) payload.get("pass");
        boolean pass = passObj != null && passObj;

        String reason = (String) payload.get("reason");

        orderService.auditRefund(id, pass, reason, adminUsername);
        return ResponseEntity.ok("审核处理完成");
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<String> payOrder(@PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String method = (payload != null && payload.get("method") != null) ? payload.get("method") : "NORMAL";
        orderService.payOrder(id, username, method);
        return ResponseEntity.ok("支付成功");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteOrder(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        OrderRecord order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!order.getUsername().equals(username)) {
            return ResponseEntity.status(403).body("无权删除此订单");
        }

        if ("售后处理中".equals(order.getStatus())) {
            return ResponseEntity.badRequest().body("售后处理中的订单不可删除");
        }

        orderRepository.delete(order);
        return ResponseEntity.ok("订单已删除");
    }
}