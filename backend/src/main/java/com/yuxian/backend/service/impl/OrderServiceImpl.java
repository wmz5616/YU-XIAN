package com.yuxian.backend.service.impl;

import com.yuxian.backend.entity.*;
import com.yuxian.backend.repository.*;
import com.yuxian.backend.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public OrderServiceImpl(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(String username, List<Map<String, Object>> itemPayloads) {
        double total = 0.0;
        OrderRecord order = new OrderRecord();
        order.setUsername(username);
        order.setCreateTime(java.time.LocalDateTime.now());
        order.setStatus("待发货");
        
        List<OrderItem> orderItems = new ArrayList<>();

        StringBuilder namesBuilder = new StringBuilder();

        for (Map<String, Object> payload : itemPayloads) {
            Long pid = Long.valueOf(payload.get("id").toString());
            int quantity = Integer.parseInt(payload.get("quantity").toString());

            Product product = productRepository.findById(pid)
                    .orElseThrow(() -> new RuntimeException("商品不存在: " + pid));

            if (product.getStock() < quantity) {
                throw new RuntimeException("商品 " + product.getName() + " 库存不足！");
            }

            // 扣减库存
            product.setStock(product.getStock() - quantity);
            productRepository.save(product);

            // 组装订单项
            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setImageUrl(product.getImageUrl());
            item.setPrice(product.getPrice());
            item.setQuantity(quantity);
            item.setOrder(order); // 关联父订单
            
            orderItems.add(item);
            total += product.getPrice() * quantity;

            namesBuilder.append(product.getName()).append(" x").append(quantity).append(", ");
        }
        String names = namesBuilder.toString();

        if (names.length() > 2) {
            names = names.substring(0, names.length() - 2);
        }
        order.setProductNames(names);

        if (total <= 200.0) {
            total += 20.0;
        }

        order.setItems(orderItems);
        order.setTotalPrice(Double.parseDouble(String.format("%.2f", total)));
        
        orderRepository.save(order);
    }
}