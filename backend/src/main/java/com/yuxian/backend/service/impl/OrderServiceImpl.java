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

import com.yuxian.backend.dto.RefundDetailVO;

@Service
public class OrderServiceImpl implements OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserCouponRepository userCouponRepository;
    private final RefundFeedbackRepository refundFeedbackRepository;
    private final UserRepository userRepository;
    private final WalletLogRepository walletLogRepository;

    public OrderServiceImpl(ProductRepository productRepository,
            OrderRepository orderRepository,
            UserCouponRepository userCouponRepository,
            RefundFeedbackRepository refundFeedbackRepository,
            UserRepository userRepository,
            WalletLogRepository walletLogRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userCouponRepository = userCouponRepository;
        this.refundFeedbackRepository = refundFeedbackRepository;
        this.userRepository = userRepository;
        this.walletLogRepository = walletLogRepository;
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
    public void payOrder(Long orderId, String username, String paymentMethod) {
        OrderRecord order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!order.getUsername().equals(username)) {
            throw new RuntimeException("无权操作此订单");
        }

        if (!"UNPAID".equals(order.getStatus())) {
            throw new RuntimeException("订单状态异常，无法支付");
        }

        if ("BALANCE".equals(paymentMethod)) {
            User user = userRepository.findByUsername(username);
            if (user == null)
                throw new RuntimeException("用户异常");

            BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
            if (currentBalance.compareTo(order.getTotalPrice()) < 0) {
                throw new RuntimeException("余额不足，请充值或选择其他支付方式");
            }

            user.setBalance(currentBalance.subtract(order.getTotalPrice()));
            userRepository.save(user);

            WalletLog log = new WalletLog();
            log.setUserId(user.getId());
            log.setAmount(order.getTotalPrice().negate());
            log.setType(2);
            log.setDescription("购买商品：" + order.getProductNames());
            walletLogRepository.save(log);

            System.out.println("余额支付成功: User=" + username + " Price=" + order.getTotalPrice());
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
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long orderId, String reason, String type, String username) {

        OrderRecord order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!order.getUsername().equals(username)) {
            throw new RuntimeException("无权操作此订单");
        }

        if (!"已送达".equals(order.getStatus()) && !"DELIVERED".equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不可申请售后");
        }

        order.setStatus("售后处理中");
        orderRepository.save(order);

        RefundFeedback feedback = new RefundFeedback();
        feedback.setOrderId(orderId);
        feedback.setType("仅退款".equals(type) ? 1 : 2);
        feedback.setContent("用户申请售后：" + reason);
        feedback.setOperator(username);
        refundFeedbackRepository.save(feedback);

        try {
            WebSocketServer.sendInfo("NEW_REFUND");
        } catch (Exception e) {
            System.err.println("WebSocket 推送失败: " + e.getMessage());
        }
    }

    @Override
    public List<OrderRecord> getPendingRefundOrders() {
        return orderRepository.findByStatusOrderByCreateTimeDesc("售后处理中");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRefund(Long orderId, boolean pass, String reason, String adminUsername) {

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
            adminFeedback.setContent("审核通过");

            User user = userRepository.findByUsername(order.getUsername());
            if (user != null) {
                BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
                user.setBalance(currentBalance.add(order.getTotalPrice()));
                userRepository.save(user);

                WalletLog log = new WalletLog();
                log.setUserId(user.getId());
                log.setAmount(order.getTotalPrice());
                log.setType(1);
                log.setDescription("订单退款: " + orderId);
                walletLogRepository.save(log);

                WebSocketServer.sendToUser(user.getUsername(), "【系统消息】您的订单退款已到账，金额：" + order.getTotalPrice());
            }

            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    productRepository.increaseStock(item.getProductId(), item.getQuantity());
                }
            }
        } else {
            order.setStatus("已送达");
            adminFeedback.setContent("审核驳回，原因：" + (reason != null ? reason : "无"));
        }

        refundFeedbackRepository.save(adminFeedback);
        orderRepository.save(order);
    }

    @Override
    public List<RefundDetailVO> getPendingRefundsWithDetails() {

        List<OrderRecord> orders = orderRepository.findByStatusOrderByCreateTimeDesc("售后处理中");

        List<RefundDetailVO> result = new ArrayList<>();

        for (OrderRecord order : orders) {

            List<RefundFeedback> feedbacks = refundFeedbackRepository.findByOrderId(order.getId());

            String reason = "无详细原因";
            if (!feedbacks.isEmpty()) {
                reason = feedbacks.get(feedbacks.size() - 1).getContent();
            }

            result.add(new RefundDetailVO(
                    order.getId(),
                    order.getUsername(),
                    order.getTotalPrice(),
                    reason,
                    order.getStatus(),
                    order.getProductNames(),
                    order.getCreateTime()));
        }
        return result;
    }

    @Override
    public List<OrderRecord> getMyOrders(String username) {
        return orderRepository.findByUsernameOrderByCreateTimeDesc(username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long orderId, String username) {
        OrderRecord order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!order.getUsername().equals(username)) {
            throw new RuntimeException("无权操作此订单");
        }

        String status = order.getStatus();
        if (!"UNPAID".equals(status) && !"CANCELLED".equals(status)) {
            throw new RuntimeException("当前订单状态不允许删除或取消");
        }

        if ("UNPAID".equals(status) && order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                productRepository.increaseStock(item.getProductId(), item.getQuantity());
            }
        }

        orderRepository.delete(order);
    }
}