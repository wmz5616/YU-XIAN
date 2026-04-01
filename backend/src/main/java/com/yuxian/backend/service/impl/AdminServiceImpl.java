package com.yuxian.backend.service.impl;

import com.yuxian.backend.entity.OrderRecord;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.OrderRepository;
import com.yuxian.backend.repository.ProductRepository;
import com.yuxian.backend.repository.UserRepository;
import com.yuxian.backend.service.AdminService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public AdminServiceImpl(UserRepository userRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();

        BigDecimal totalSalesDecimal = orderRepository.sumTotalSales();
        double totalSales = (totalSalesDecimal != null) ? totalSalesDecimal.doubleValue() : 0.0;

        long totalOrders = orderRepository.count();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<String> invalidStatuses = List.of("UNPAID", "CANCELLED", "REFUNDED", "PENDING_REFUND");
        List<OrderRecord> recentOrders = orderRepository.findByCreateTimeAfter(sevenDaysAgo).stream()
                .filter(o -> !invalidStatuses.contains(o.getStatus()))
                .collect(Collectors.toList());

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

        return stats;
    }

    @Override
    public Map<String, Object> getOrders(int page, int size, String keyword, String status) {
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.DESC, "createTime");
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page - 1,
                size, sort);

        org.springframework.data.domain.Page<OrderRecord> orderPage;

        boolean hasKeyword = keyword != null && !keyword.isEmpty();
        boolean hasStatus = status != null && !status.isEmpty() && !"ALL".equals(status);

        if (hasKeyword && hasStatus) {
            orderPage = orderRepository.findByUsernameContainingAndStatusOrderByCreateTimeDesc(keyword, status,
                    pageable);
        } else if (hasKeyword) {
            orderPage = orderRepository.findByUsernameContainingOrderByCreateTimeDesc(keyword, pageable);
        } else if (hasStatus) {
            orderPage = orderRepository.findByStatusOrderByCreateTimeDesc(status, pageable);
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

    @Override
    public void updateOrderStatus(Long id, String status) {
        orderRepository.findById(id).ifPresent(order -> {
            order.setStatus(status);
            orderRepository.save(order);
        });
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public void updateUserPoints(Long id, Integer points) {
        userRepository.findById(id).ifPresent(user -> {
            user.setPoints(points);
            userRepository.save(user);
        });
    }
}
