package com.yuxian.backend.controller;

import com.yuxian.backend.entity.OrderRecord;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.OrderRepository;
import com.yuxian.backend.repository.ProductRepository;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/orders")
    public Map<String, Object> getOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "") String keyword) {

        // 注意：Spring Data JPA 的页码是从 0 开始的，前端传 1 要减 1
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<OrderRecord> orderPage;
        if (keyword != null && !keyword.isEmpty()) {
            // 如果有关键词，就搜人名
            orderPage = orderRepository.findByUsernameContainingOrderByCreateTimeDesc(keyword, pageable);
        } else {
            // 否则查全部
            orderPage = orderRepository.findAll(pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", orderPage.getContent()); // 当前页的数据列表
        response.put("totalElements", orderPage.getTotalElements()); // 总条数 (用于前端计算总页数)
        response.put("totalPages", orderPage.getTotalPages()); // 总页数
        response.put("currentPage", page); // 当前页码

        return response;
    }

    // 3. 仪表盘统计接口
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. 基础计数
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();

        // 获取所有订单
        List<OrderRecord> allOrders = orderRepository.findAll();

        // 2. 计算总销售额 (防止 NPE)
        double totalSales = allOrders.stream()
                .filter(o -> o.getTotalPrice() != null)
                .mapToDouble(OrderRecord::getTotalPrice)
                .sum();

        // 3. 【核心新增】计算近7天的销售趋势
        // 我们需要返回两个数组：dateList (日期 ["12-10", "12-11"...]) 和 valueList (金额 [100.0,
        // 20.0...])

        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // 过滤近7天的订单，并按日期分组求和
        Map<String, Double> salesTrend = allOrders.stream()
                .filter(o -> o.getCreateTime() != null && o.getCreateTime().isAfter(sevenDaysAgo))
                .collect(Collectors.groupingBy(
                        o -> o.getCreateTime().format(formatter),
                        LinkedHashMap::new, // 保持插入顺序其实还是要靠排序，下面会处理
                        Collectors.summingDouble(OrderRecord::getTotalPrice)));

        // 构造有序的最近7天数据（防止某天没订单导致断层）
        List<String> dateList = new ArrayList<>();
        List<Double> valueList = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            String dateKey = LocalDateTime.now().minusDays(i).format(formatter);
            dateList.add(dateKey);
            // 如果那天没数据，就填 0.0
            valueList.add(salesTrend.getOrDefault(dateKey, 0.0));
        }

        stats.put("totalUsers", totalUsers);
        stats.put("totalProducts", totalProducts);
        stats.put("totalOrders", totalOrders);
        stats.put("totalSales", totalSales);
        // 新增图表数据
        stats.put("chartData", Map.of("dates", dateList, "values", valueList));

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

    @PutMapping("/users/{id}/points")
    public ResponseEntity<?> updateUserPoints(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer points = body.get("points");
        if (points == null)
            return ResponseEntity.badRequest().body("Points required");

        return userRepository.findById(id).map(user -> {
            user.setPoints(points); // 直接设置新积分，或者你可以做成累加逻辑
            userRepository.save(user);
            return ResponseEntity.ok(user);
        }).orElse(ResponseEntity.notFound().build());
    }
}