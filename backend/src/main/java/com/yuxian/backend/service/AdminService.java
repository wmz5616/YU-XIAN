package com.yuxian.backend.service;

import java.util.Map;
import com.yuxian.backend.entity.User;
import java.util.List;

public interface AdminService {
    Map<String, Object> getDashboardStats();
    List<User> getAllUsers();
    void deleteUser(Long id);
    void updateUserPoints(Long id, Integer points);
    Map<String, Object> getOrders(int page, int size, String keyword, String status);
    void updateOrderStatus(Long id, String status);
}
