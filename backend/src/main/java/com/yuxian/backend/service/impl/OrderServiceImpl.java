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
    private final UserCouponRepository userCouponRepository;
    // ✅ 新增注入：售后反馈仓库
    private final RefundFeedbackRepository refundFeedbackRepository;

    public OrderServiceImpl(ProductRepository productRepository,
            OrderRepository orderRepository,
            UserCouponRepository userCouponRepository,
            // ✅ 构造函数注入 RefundFeedbackRepository
            RefundFeedbackRepository refundFeedbackRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userCouponRepository = userCouponRepository;
        this.refundFeedbackRepository = refundFeedbackRepository; // ✅ 注入赋值
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        // 5. 核心修改：处理优惠券扣减逻辑
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
    @Transactional
    // ✅ 修改方法签名：新增 username 参数
    public void applyRefund(Long orderId, String reason, String type, String username) {
        // 1. 找订单
        OrderRecord order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 2. 校验状态 (只有“已送达”的订单才能申请售后)
        if (!"已送达".equals(order.getStatus()) && !"DELIVERED".equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不可申请售后");
        }

        // 3. 记录用户申请反馈 (新增逻辑)
        RefundFeedback userFeedback = new RefundFeedback();
        userFeedback.setOrderId(orderId);
        userFeedback.setType(0); // 0: 用户申请
        userFeedback.setContent("申请类型: " + type + " / 原因: " + reason);
        userFeedback.setOperator(username); // 记录用户操作人
        refundFeedbackRepository.save(userFeedback);

        // 4. 更新状态
        order.setStatus("售后处理中");

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
    @Transactional // ✅ 确保事务开启，所有数据库操作要么全成功要么全回滚
    public void auditRefund(Long orderId, boolean pass, String rejectReason, String adminUsername) {
        OrderRecord order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!"售后处理中".equals(order.getStatus())) {
            throw new RuntimeException("该订单当前不在售后流程中");
        }

        // 1. 准备管理员反馈记录 (持久化反馈内容)
        RefundFeedback adminFeedback = new RefundFeedback();
        adminFeedback.setOrderId(orderId);
        adminFeedback.setType(1);
        adminFeedback.setOperator(adminUsername);

        if (pass) {
            order.setStatus("退款成功"); // ✅ 修改状态
            adminFeedback.setContent("审核通过，已完成退款处理。");
            // 库存回滚逻辑...
            for (OrderItem item : order.getItems()) {
                productRepository.increaseStock(item.getProductId(), item.getQuantity());
            }
        } else {
            order.setStatus("已送达"); // ✅ 驳回则恢复状态
            adminFeedback.setContent("审核驳回，原因：" + rejectReason);
        }

        // 2. ✅ 核心修复：显式保存反馈记录和订单状态
        refundFeedbackRepository.save(adminFeedback);
        orderRepository.save(order); // 👈 必须执行这一行，否则刷新后状态变回原样
    }
}