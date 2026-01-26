package com.yuxian.backend.task;

import com.yuxian.backend.entity.OrderItem;
import com.yuxian.backend.entity.OrderRecord;
import com.yuxian.backend.repository.OrderRepository;
import com.yuxian.backend.repository.ProductRepository;
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

        System.out.println(">>> Finding " + timeoutOrders.size() + " timeout orders...");

        for (OrderRecord order : timeoutOrders) {
            System.out.println("Auto-cancelling order: " + order.getId());

            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    productRepository.increaseStock(item.getProductId(), item.getQuantity());
                }
            }

            order.setStatus("CANCELLED");
            orderRepository.save(order);
        }
    }
}
