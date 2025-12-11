package com.yuxian.backend.service;

import java.util.Map;

public interface OrderService {
    void createOrder(String username, java.util.List<Map<String, Object>> items);
}