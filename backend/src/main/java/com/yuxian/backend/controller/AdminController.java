package com.yuxian.backend.controller;

import com.yuxian.backend.entity.OrderRecord;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.OrderRepository;
import com.yuxian.backend.repository.ProductRepository;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 后台管理控制器
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*") // 允许跨域
public class AdminController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public AdminController(UserRepository userRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    // 1. 获取所有用户
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 2. 获取所有订单
    @GetMapping("/orders")
    public List<OrderRecord> getAllOrders() {
        return orderRepository.findAll();
    }

    // 3. 仪表盘统计接口
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();

        // 计算总销售额 (使用流式计算，防止空指针)
        List<OrderRecord> orders = orderRepository.findAll();
        double totalSales = orders.stream()
                .filter(o -> o.getTotalPrice() != null) // 确保金额不为空
                .mapToDouble(OrderRecord::getTotalPrice)
                .sum();

        stats.put("totalUsers", totalUsers);
        stats.put("totalProducts", totalProducts);
        stats.put("totalOrders", totalOrders);
        stats.put("totalSales", totalSales);

        return ResponseEntity.ok(stats);
    }

    // 4. 修改订单状态 (发货)
    @PutMapping("/orders/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<OrderRecord> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            OrderRecord order = orderOpt.get();
            String newStatus = body.get("status");
            if (newStatus != null) {
                order.setStatus(newStatus);
                orderRepository.save(order);
                return ResponseEntity.ok(order);
            }
        }
        return ResponseEntity.badRequest().body("Order not found or status missing");
    }

    // 5. 删除用户
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}