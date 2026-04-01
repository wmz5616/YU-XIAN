package com.yuxian.backend.controller;

import com.yuxian.backend.entity.User;
import com.yuxian.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return adminService.getAllUsers();
    }

    @GetMapping("/orders")
    public Map<String, Object> getOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "ALL") String status
        ) {
        return adminService.getOrders(page, size, keyword, status);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus != null) {
            adminService.updateOrderStatus(id, newStatus);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("Status missing");
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/points")
    public ResponseEntity<?> updateUserPoints(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer points = body.get("points");
        if (points == null)
            return ResponseEntity.badRequest().body("Points required");

        adminService.updateUserPoints(id, points);
        return ResponseEntity.ok().build();
    }
}