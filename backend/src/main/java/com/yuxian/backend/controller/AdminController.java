package com.yuxian.backend.controller;

import com.yuxian.backend.entity.OrderRecord;
import com.yuxian.backend.entity.Product;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.OrderRepository;
import com.yuxian.backend.repository.ProductRepository;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public AdminController(OrderRepository orderRepository, UserRepository userRepository,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    private boolean isAdmin(String username) {
        User user = userRepository.findByUsername(username);
        return user != null && "ADMIN".equals(user.getRole());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats(@RequestParam String username) {
        if (!isAdmin(username))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权访问");

        long totalOrders = orderRepository.count();
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();

        List<OrderRecord> allOrders = orderRepository.findAll();
        double totalSales = allOrders.stream().mapToDouble(OrderRecord::getTotalPrice).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", totalOrders);
        stats.put("totalUsers", totalUsers);
        stats.put("totalProducts", totalProducts);
        stats.put("totalSales", String.format("%.2f", totalSales));
        stats.put("pendingOrders", allOrders.stream().filter(o -> "待发货".equals(o.getStatus())).count());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders(@RequestParam String username) {
        if (!isAdmin(username))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权访问");

        List<OrderRecord> orders = orderRepository.findAll();
        orders.sort((o1, o2) -> o2.getCreateTime().compareTo(o1.getCreateTime()));

        return ResponseEntity.ok(orders);
    }

    @PostMapping("/orders/{id}/ship")
    public ResponseEntity<?> shipOrder(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        if (!isAdmin(username))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权访问");

        OrderRecord order = orderRepository.findById(id).orElse(null);
        if (order == null)
            return ResponseEntity.badRequest().body("订单不存在");

        if ("待发货".equals(order.getStatus())) {
            order.setStatus("运输中");
            orderRepository.save(order);
            return ResponseEntity.ok("发货成功！物流追踪已启动");
        } else {
            return ResponseEntity.badRequest().body("当前订单状态无法发货");
        }
    }

    @PostMapping("/products/{id}/restock")
    public ResponseEntity<?> restockProduct(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("username");
        if (!isAdmin(username))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权访问");

        int amount = (int) payload.get("amount");

        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            product.setStock(product.getStock() + amount);
            productRepository.save(product);
            return ResponseEntity.ok("补货成功，当前库存: " + product.getStock());
        }
        return ResponseEntity.badRequest().body("商品不存在");
    }
}