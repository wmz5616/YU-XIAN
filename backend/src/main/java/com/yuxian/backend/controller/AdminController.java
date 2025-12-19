package com.yuxian.backend.controller;

import com.yuxian.backend.entity.OrderRecord;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.OrderRepository;
import com.yuxian.backend.repository.ProductRepository;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/admin")
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

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/orders")
    public Map<String, Object> getOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "") String keyword) {

        Pageable pageable = PageRequest.of(page - 1, size);

        Page<OrderRecord> orderPage;
        if (keyword != null && !keyword.isEmpty()) {
            orderPage = orderRepository.findByUsernameContainingOrderByCreateTimeDesc(keyword, pageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", orderPage.getContent());
        response.put("totalElements", orderPage.getTotalElements());
        response.put("totalPages", orderPage.getTotalPages());
        response.put("currentPage", page);

        return response;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();

        BigDecimal totalSalesDecimal = orderRepository.sumTotalSales();

        double totalSales = (totalSalesDecimal != null) ? totalSalesDecimal.doubleValue() : 0.0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<OrderRecord> recentOrders = orderRepository.findByCreateTimeAfter(sevenDaysAgo);

        Map<String, Double> salesTrend = recentOrders.stream()
                .filter(o -> o.getTotalPrice() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getCreateTime().format(formatter),
                        Collectors.summingDouble(o -> o.getTotalPrice().doubleValue())));

        List<String> dateList = new ArrayList<>();
        List<Double> valueList = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            String dateKey = LocalDateTime.now().minusDays(i).format(formatter);
            dateList.add(dateKey);
            valueList.add(salesTrend.getOrDefault(dateKey, 0.0));
        }

        stats.put("totalUsers", totalUsers);
        stats.put("totalProducts", totalProducts);
        stats.put("totalOrders", totalOrders);
        stats.put("totalSales", totalSales);
        stats.put("chartData", Map.of("dates", dateList, "values", valueList));

        return ResponseEntity.ok(stats);
    }

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

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/points")
    public ResponseEntity<?> updateUserPoints(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer points = body.get("points");
        if (points == null)
            return ResponseEntity.badRequest().body("Points required");

        return userRepository.findById(id).map(user -> {
            user.setPoints(points);
            userRepository.save(user);
            return ResponseEntity.ok(user);
        }).orElse(ResponseEntity.notFound().build());
    }
}