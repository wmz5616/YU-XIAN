package com.yuxian.backend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.yuxian.backend.entity.OrderRecord;
import com.yuxian.backend.entity.Product;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.ProductRepository;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin
public class ProductController {

    private final ProductRepository productRepository;
    private final com.yuxian.backend.repository.OrderRepository orderRepository;
    private final UserRepository userRepository;

    public ProductController(ProductRepository productRepository, 
                             com.yuxian.backend.repository.OrderRepository orderRepository,
                             UserRepository userRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/category/{type}")
    public List<Product> getProductsByCategory(@PathVariable String type) {
        return productRepository.findByCategory(type);
    }

    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam String keyword) {
        return productRepository.findByNameContaining(keyword);
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @GetMapping("/{id}/insight")
    public Map<String, Object> getProductInsight(@PathVariable Long id) {
        Product product = productRepository.findById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        
        if (product == null) return result;

        List<Map<String, Object>> priceHistory = new ArrayList<>();
        double basePrice = product.getPrice();
        LocalDate today = LocalDate.now();
        Random random = new Random(id); // 使用ID作为种子，保证每次刷新看到的波动是一样的（稳定真实感）

        for (int i = 6; i >= 0; i--) {
            Map<String, Object> point = new HashMap<>();
            LocalDate date = today.minusDays(i);
            double factor = 0.9 + (random.nextDouble() * 0.2); 
            double dailyPrice = Math.round(basePrice * factor * 100.0) / 100.0;
            
            point.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
            point.put("price", dailyPrice);
            priceHistory.add(point);
        }
        result.put("priceHistory", priceHistory);

        List<Map<String, Object>> traceEvents = new ArrayList<>();
        LocalDate catchDate = product.getListDate().minusDays(2);

        traceEvents.add(createTraceEvent(
            catchDate.toString() + " 04:30", 
            "捕捞作业完成", 
            "作业渔船：浙普渔" + (60000 + id * 123) + "号 | 海域：东经122.5° 北纬30.2°"
        ));
        
        traceEvents.add(createTraceEvent(
            catchDate.plusDays(1).toString() + " 09:15", 
            "舟山港口卸货入库", 
            "检测批次：NO." + (20250000 + id) + " | 鲜度等级：特A级"
        ));
        
        traceEvents.add(createTraceEvent(
            product.getListDate().toString() + " 02:00", 
            "全程冷链运输中", 
            "运输车牌：浙L·" + (8888 + id) + " | 实时库温：-18.5°C"
        ));

        traceEvents.add(createTraceEvent(
            product.getListDate().toString() + " 08:00", 
            "到达城市前置仓", 
            "已完成核酸检测与消杀，准备上架"
        ));

        result.put("traceEvents", traceEvents);
        result.put("blockchainHash", "0x" + Long.toHexString(Double.doubleToLongBits(Math.random())).toUpperCase() + "...VERIFIED");

        return result;
    }

    @DeleteMapping("/order/{id}")
    public String deleteOrder(@PathVariable Long id) {
        orderRepository.deleteById(id);
        return "订单已删除";
    }

    private Map<String, Object> createTraceEvent(String time, String title, String desc) {
        Map<String, Object> event = new HashMap<>();
        event.put("time", time);
        event.put("title", title);
        event.put("desc", desc);
        return event;
    }

    @PostMapping("/order")
    @Transactional(rollbackFor = Exception.class) 
    public String createOrder(@RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("username");
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

        double total = 0.0;
        StringBuilder productNames = new StringBuilder();

        for (Map<String, Object> item : items) {
            Long pid = Long.valueOf(item.get("id").toString());
            int quantity = Integer.parseInt(item.get("quantity").toString());

            Product product = productRepository.findById(pid)
                    .orElseThrow(() -> new RuntimeException("商品不存在: " + pid));

            if (product.getStock() < quantity) {
                throw new RuntimeException("商品 " + product.getName() + " 库存不足！");
            }

            product.setStock(product.getStock() - quantity);
            productRepository.save(product);

            total += product.getPrice() * quantity;
            productNames.append(product.getName()).append(" x").append(quantity).append(", ");
        }

        if (total <= 200.0) {
            total += 20.0;
        }

        OrderRecord order = new OrderRecord();
        order.setUsername(username);
        
        String names = productNames.toString();
        if (names.length() > 2) {
            names = names.substring(0, names.length() - 2);
        }
        order.setProductNames(names);
        order.setTotalPrice(Double.parseDouble(String.format("%.2f", total)));
        order.setStatus("待发货");
        order.setCreateTime(java.time.LocalDateTime.now());
        orderRepository.save(order);

        return "下单成功";
    }

    @PostMapping("/order/{id}/receive")
    @Transactional
    public ResponseEntity<?> confirmReceipt(@PathVariable Long id) {
        OrderRecord order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            return ResponseEntity.badRequest().body("订单不存在");
        }

        if ("已送达".equals(order.getStatus())) {
            return ResponseEntity.badRequest().body("订单已完成，请勿重复操作");
        }

        order.setStatus("已送达");
        orderRepository.save(order);

        int pointsToAdd = order.getTotalPrice().intValue();

        User user = userRepository.findByUsername(order.getUsername());
        if (user != null) {
            if (user.getPoints() == null) user.setPoints(0);
            user.setPoints(user.getPoints() + pointsToAdd);
            userRepository.save(user);
            return ResponseEntity.ok(user);
        }

        return ResponseEntity.ok("操作成功，但未找到用户");
    }
    
    @GetMapping("/orders")
    public java.util.List<OrderRecord> getMyOrders(@RequestParam String username) {
        return orderRepository.findByUsernameOrderByCreateTimeDesc(username);
    }

    @GetMapping("/recommend")
    public List<Product> getDailyRecommendations() {
        return productRepository.findRandomRecommendations();
    }
}