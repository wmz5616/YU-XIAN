package com.yuxian.backend.controller;

import com.yuxian.backend.entity.Address;
import com.yuxian.backend.entity.OrderRecord;
import com.yuxian.backend.entity.RefundFeedback;
import com.yuxian.backend.service.OrderService;
import com.yuxian.backend.repository.RefundFeedbackRepository; // ✅ 新增导入

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections; // ✅ 修复 Collections 找不到的错误
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final RefundFeedbackRepository refundFeedbackRepository; // ✅ 新增注入

    public OrderController(OrderService orderService, RefundFeedbackRepository refundFeedbackRepository) {
        this.orderService = orderService;
        this.refundFeedbackRepository = refundFeedbackRepository;
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

    // ✅ 修复：处理售后申请 (新增 username 参数)
    @PostMapping("/{id}/refund")
    public ResponseEntity<String> applyRefund(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String reason = payload.get("reason");
        String type = payload.get("type");
        String username = payload.get("username"); // ✅ 假设 username 从 Payload 传入

        if (username == null) {
            return ResponseEntity.badRequest().body("Username required for refund application.");
        }

        // ✅ 传入 username
        orderService.applyRefund(id, reason, type, username);

        return ResponseEntity.ok("售后申请已提交");
    }

    @GetMapping("/admin/refunds")
    public List<OrderRecord> getRefundOrders() {
        return orderService.getPendingRefundOrders();
    }

    // ✅ 修复：审核售后 (新增 adminUsername 参数)
    @PostMapping("/admin/refunds/{id}/audit") // ✅ 匹配路径
public ResponseEntity<String> auditRefund(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
    boolean pass = (boolean) payload.get("pass");
    String reason = (String) payload.get("reason");
    String adminUsername = (String) payload.get("adminUsername");

    if (adminUsername == null) {
        return ResponseEntity.badRequest().body("操作人不能为空");
    }

    // ✅ 调用 Service 进行持久化处理
    orderService.auditRefund(id, pass, reason, adminUsername);
    
    return ResponseEntity.ok(pass ? "已同意退款" : "已驳回申请");
}

    // ✅ 修复：获取反馈历史接口
    @GetMapping("/{orderId}/feedback")
    public List<RefundFeedback> getRefundFeedback(@PathVariable Long orderId) {
        if (orderId == null) {
            return Collections.emptyList();
        }
        // 实际查询数据库
        return refundFeedbackRepository.findByOrderIdOrderByCreateTimeAsc(orderId);
    }
}