package com.yuxian.backend.service.impl;

import com.yuxian.backend.entity.*;
import com.yuxian.backend.repository.*;
import com.yuxian.backend.service.OrderService;
import com.yuxian.backend.service.WebSocketServer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserCouponRepository userCouponRepository; // ✅ 新增注入

    public OrderServiceImpl(ProductRepository productRepository,
            OrderRepository orderRepository,
            UserCouponRepository userCouponRepository) { // ✅ 构造函数注入
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userCouponRepository = userCouponRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // ✅ 修改：方法签名必须包含 Long couponId
    public void createOrder(String username, List<Map<String, Object>> itemPayloads, Address addressSnapshot,
            Long couponId) {

        // 1. 基础校验
        if (itemPayloads == null || itemPayloads.isEmpty()) {
            throw new RuntimeException("订单商品不能为空");
        }
        if (addressSnapshot == null) {
            throw new RuntimeException("收货地址不能为空");
        }

        // 2. 初始化订单对象
        OrderRecord order = new OrderRecord();
        order.setUsername(username);
        order.setCreateTime(LocalDateTime.now());
        order.setStatus("PAID");

        // 保存地址快照
        order.setReceiverName(addressSnapshot.getContact());
        order.setReceiverPhone(addressSnapshot.getPhone());
        order.setReceiverAddress(addressSnapshot.getDetail());

        // 3. 处理订单项
        List<OrderItem> orderItems = new ArrayList<>();
        StringBuilder namesBuilder = new StringBuilder();
        double total = 0.0;

        for (Map<String, Object> payload : itemPayloads) {
            Long pid = Long.valueOf(payload.get("id").toString());
            int quantity = Integer.parseInt(payload.get("quantity").toString());

            Product product = productRepository.findById(pid)
                    .orElseThrow(() -> new RuntimeException("商品不存在: " + pid));

            // 扣库存
            int rows = productRepository.decreaseStock(pid, quantity);
            if (rows == 0) {
                throw new RuntimeException("商品 [" + product.getName() + "] 库存不足！");
            }

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setImageUrl(product.getImageUrl());
            item.setPrice(product.getPrice());
            item.setQuantity(quantity);
            item.setOrder(order);

            orderItems.add(item);
            total += product.getPrice() * quantity;
            namesBuilder.append(product.getName()).append(" x").append(quantity).append(", ");
        }

        String names = namesBuilder.toString();
        if (names.length() > 2)
            names = names.substring(0, names.length() - 2);
        order.setProductNames(names);

        // 4. 计算运费
        if (total <= 200.0) {
            total += 20.0;
        }

        // ✅ 5. 核心修改：处理优惠券扣减逻辑
        if (couponId != null) {
            // 查券
            UserCoupon userCoupon = userCouponRepository.findById(couponId)
                    .orElseThrow(() -> new RuntimeException("优惠券不存在"));

            // 校验归属人
            if (!userCoupon.getUsername().equals(username)) {
                throw new RuntimeException("优惠券归属错误");
            }

            // 校验状态
            if (!"UNUSED".equals(userCoupon.getStatus())) {
                throw new RuntimeException("该优惠券已使用或已过期");
            }

            // 校验门槛
            if (total < userCoupon.getMinSpend()) {
                throw new RuntimeException("未满足优惠券使用门槛");
            }

            // 执行减钱
            System.out.println(">>> 使用优惠券: " + userCoupon.getCouponName() + " 减免: " + userCoupon.getAmount());
            total = total - userCoupon.getAmount();
            if (total < 0)
                total = 0.0; // 防止负数

            // 标记券为已使用
            userCoupon.setStatus("USED");
            userCouponRepository.save(userCoupon);
        }

        // 6. 保存订单
        order.setItems(orderItems);
        order.setTotalPrice(Double.parseDouble(String.format("%.2f", total)));
        orderRepository.save(order);

        // 7. 发送 WebSocket 通知
        try {
            WebSocketServer.sendInfo("NEW_ORDER");
        } catch (Exception e) {
            System.err.println("WebSocket 推送失败: " + e.getMessage());
        }
    }

    @Override
    public void applyRefund(Long orderId, String reason, String type) {
        // 1. 找订单
        OrderRecord order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 2. 校验状态 (只有“已送达”的订单才能申请售后)
        if (!"已送达".equals(order.getStatus()) && !"DELIVERED".equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不可申请售后");
        }

        // 3. 更新状态
        // 这里简单处理，直接改状态。实际项目中可能会存到一张单独的“售后申请表”
        order.setStatus("售后处理中");

        // 4. (可选) 您可以将 reason 和 type 存入备注字段，或者打印日志
        System.out.println("订单 " + orderId + " 申请售后: " + type + ", 原因: " + reason);

        orderRepository.save(order);
    }

    @Override
    public List<OrderRecord> getPendingRefundOrders() {
        // 查询所有状态为 "售后处理中" 的订单
        return orderRepository.findAll().stream()
                .filter(o -> "售后处理中".equals(o.getStatus()))
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime())) // 按时间倒序
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void auditRefund(Long orderId, boolean pass, String rejectReason) {
        OrderRecord order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!"售后处理中".equals(order.getStatus())) {
            throw new RuntimeException("该订单当前不在售后流程中");
        }

        if (pass) {
            // === 审核通过 ===
            order.setStatus("退款成功");

            // 【可选优化】: 这里可以写代码把库存加回去
            // for (OrderItem item : order.getItems()) {
            // productRepository.increaseStock(item.getProductId(), item.getQuantity());
            // }

            System.out.println("订单 " + orderId + " 退款审核通过");
        } else {
            // === 审核驳回 ===
            // 状态恢复为 "已送达" 或者专门的 "售后驳回"
            order.setStatus("已送达");
            // 如果实体类里有备注字段，可以保存 rejectReason
            System.out.println("订单 " + orderId + " 退款驳回，原因: " + rejectReason);
        }

        orderRepository.save(order);
    }
}