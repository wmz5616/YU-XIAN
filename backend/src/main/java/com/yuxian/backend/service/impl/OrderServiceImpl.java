package com.yuxian.backend.service.impl;

import com.yuxian.backend.entity.*;
import com.yuxian.backend.repository.*;
import com.yuxian.backend.service.OrderService;
import com.yuxian.backend.service.WebSocketServer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final RefundFeedbackRepository refundFeedbackRepository;

    public OrderServiceImpl(ProductRepository productRepository,
            OrderRepository orderRepository,
            UserCouponRepository userCouponRepository,
            RefundFeedbackRepository refundFeedbackRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userCouponRepository = userCouponRepository;
        this.refundFeedbackRepository = refundFeedbackRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(String username, List<Map<String, Object>> itemPayloads, Address addressSnapshot,
            Long couponId) {

        if (itemPayloads == null || itemPayloads.isEmpty()) {
            throw new RuntimeException("订单商品不能为空");
        }
        if (addressSnapshot == null) {
            throw new RuntimeException("收货地址不能为空");
        }

        OrderRecord order = new OrderRecord();
        order.setUsername(username);
        order.setCreateTime(LocalDateTime.now());

        order.setStatus("UNPAID");

        order.setReceiverName(addressSnapshot.getContact());
        order.setReceiverPhone(addressSnapshot.getPhone());
        order.setReceiverAddress(addressSnapshot.getDetail());

        List<OrderItem> orderItems = new ArrayList<>();
        StringBuilder namesBuilder = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;

        for (Map<String, Object> payload : itemPayloads) {
            Long pid = Long.valueOf(payload.get("id").toString());
            int quantity = Integer.parseInt(payload.get("quantity").toString());

            Product product = productRepository.findById(pid)
                    .orElseThrow(() -> new RuntimeException("商品不存在: " + pid));

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

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            total = total.add(itemTotal);

            namesBuilder.append(product.getName()).append(" x").append(quantity).append(", ");
        }

        String names = namesBuilder.toString();
        if (names.length() > 2)
            names = names.substring(0, names.length() - 2);
        order.setProductNames(names);

        BigDecimal subtotal = total;

        if (couponId != null) {
            UserCoupon userCoupon = userCouponRepository.findById(couponId)
                    .orElseThrow(() -> new RuntimeException("优惠券不存在"));

            if (!userCoupon.getUsername().equals(username)) {
                throw new RuntimeException("优惠券归属错误");
            }
            if (!"UNUSED".equals(userCoupon.getStatus())) {
                throw new RuntimeException("该优惠券已使用或已过期");
            }
            if (subtotal.compareTo(userCoupon.getMinSpend()) < 0) {
                throw new RuntimeException("未满足优惠券使用门槛 (不含运费)");
            }

            System.out.println(">>> 使用优惠券: " + userCoupon.getCouponName() + " 减免: " + userCoupon.getAmount());
            total = total.subtract(userCoupon.getAmount());
            if (total.compareTo(BigDecimal.ZERO) < 0) {
                total = BigDecimal.ZERO;
            }

            userCoupon.setStatus("USED");
            userCouponRepository.save(userCoupon);
        }

        if (subtotal.compareTo(new BigDecimal("200.0")) <= 0) {
            total = total.add(new BigDecimal("20.0"));
        }
        if (total.compareTo(BigDecimal.ZERO) < 0)
            total = BigDecimal.ZERO;

        order.setItems(orderItems);
        order.setTotalPrice(total);
        orderRepository.save(order);

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, String username) {
        OrderRecord order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!order.getUsername().equals(username)) {
            throw new RuntimeException("无权操作此订单");
        }

        if (!"UNPAID".equals(order.getStatus())) {
            throw new RuntimeException("订单状态异常，无法支付");
        }

        order.setStatus("PAID");
        orderRepository.save(order);

        try {
            WebSocketServer.sendInfo("NEW_ORDER");
        } catch (Exception e) {
            System.err.println("WebSocket 推送失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void applyRefund(Long orderId, String reason, String type, String username) {
        OrderRecord order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!"已送达".equals(order.getStatus()) && !"DELIVERED".equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不可申请售后");
        }

        LocalDateTime deadline = order.getCreateTime().plusDays(10);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new RuntimeException("已超过售后申请期限 (仅限下单后10天内)");
        }

        RefundFeedback userFeedback = new RefundFeedback();
        userFeedback.setOrderId(orderId);
        userFeedback.setType(0);
        userFeedback.setContent("申请类型: " + type + " / 原因: " + reason);
        userFeedback.setOperator(username);
        refundFeedbackRepository.save(userFeedback);

        order.setStatus("售后处理中");
        orderRepository.save(order);
    }

    @Override
    public List<OrderRecord> getPendingRefundOrders() {
        return orderRepository.findAll().stream()
                .filter(o -> "售后处理中".equals(o.getStatus()))
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void auditRefund(Long orderId, boolean pass, String rejectReason, String adminUsername) {
        OrderRecord order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!"售后处理中".equals(order.getStatus())) {
            throw new RuntimeException("该订单当前不在售后流程中");
        }

        RefundFeedback adminFeedback = new RefundFeedback();
        adminFeedback.setOrderId(orderId);
        adminFeedback.setType(1);
        adminFeedback.setOperator(adminUsername);

        if (pass) {
            order.setStatus("退款成功");
            adminFeedback.setContent("审核通过，已完成退款处理。");
            for (OrderItem item : order.getItems()) {
                productRepository.increaseStock(item.getProductId(), item.getQuantity());
            }
        } else {
            order.setStatus("已送达");
            adminFeedback.setContent("审核驳回，原因：" + rejectReason);
        }

        refundFeedbackRepository.save(adminFeedback);
        orderRepository.save(order);
    }
}