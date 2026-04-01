package com.yuxian.backend.task;

import com.yuxian.backend.entity.OrderItem;
import com.yuxian.backend.entity.OrderRecord;
import com.yuxian.backend.repository.OrderRepository;
import com.yuxian.backend.repository.ProductRepository;
import com.yuxian.backend.service.WebSocketServer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderTimeoutTask {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderTimeoutTask(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelTimeoutOrders() {

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);
        List<OrderRecord> timeoutOrders = orderRepository.findByStatusAndCreateTimeBefore("UNPAID", cutoffTime);

        if (timeoutOrders.isEmpty()) {
            return;
        }

        System.out.println(">>> 扫描到 " + timeoutOrders.size() + " 笔超时待支付订单...");

        for (OrderRecord order : timeoutOrders) {

            if (!"UNPAID".equals(order.getStatus()))
                continue;

            System.out.println("正在自动取消超时订单: ID=" + order.getId() + ", 用户=" + order.getUsername());

            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    System.out.println("  回滚库存: 商品ID=" + item.getProductId() + ", 数量=" + item.getQuantity());
                    productRepository.increaseStock(item.getProductId(), item.getQuantity());
                }
            }

            order.setStatus("CANCELLED");
            orderRepository.save(order);

            try {
                WebSocketServer.sendToUser(order.getUsername(), "【系统消息】您的订单 #" + order.getId() + " 因超时未支付已自动取消。");
            } catch (Exception e) {
                System.err.println("超时通知推送失败: " + e.getMessage());
            }
        }
    }
}
