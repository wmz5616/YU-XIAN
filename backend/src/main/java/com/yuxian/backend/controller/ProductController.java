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
import com.yuxian.backend.service.OrderService;

@RestController
@RequestMapping("/api/products")
@CrossOrigin
public class ProductController {

    private final ProductRepository productRepository;
    private final com.yuxian.backend.repository.OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    private static final Map<String, double[]> PORT_COORDINATES = new HashMap<>();
    static {
        PORT_COORDINATES.put("大连", new double[] { 121.6147, 38.9140 });
        PORT_COORDINATES.put("山东", new double[] { 120.3826, 36.0671 });
        PORT_COORDINATES.put("舟山", new double[] { 122.2965, 29.9511 });
        PORT_COORDINATES.put("东海", new double[] { 122.2965, 29.9511 });
        PORT_COORDINATES.put("福建", new double[] { 119.2965, 26.0745 });
        PORT_COORDINATES.put("广东", new double[] { 113.2644, 23.1291 });
        PORT_COORDINATES.put("湛江", new double[] { 110.3594, 21.2707 });
        PORT_COORDINATES.put("海南", new double[] { 110.1999, 20.0440 });
        PORT_COORDINATES.put("进口", new double[] { 121.4737, 31.2304 });
        PORT_COORDINATES.put("远洋", new double[] { 121.4737, 31.2304 });
        PORT_COORDINATES.put("DEFAULT", new double[] { 122.2965, 29.9511 });
    }

    public ProductController(ProductRepository productRepository,
            com.yuxian.backend.repository.OrderRepository orderRepository,
            UserRepository userRepository,
            OrderService orderService) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
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

        if (product == null)
            return result;

        List<Map<String, Object>> priceHistory = new ArrayList<>();
        double basePrice = product.getPrice();
        LocalDate today = LocalDate.now();
        Random random = new Random(id);

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
        traceEvents.add(
                createTraceEvent(catchDate.toString() + " 04:30", "捕捞作业完成", "作业渔船：浙普渔" + (60000 + id * 123) + "号"));
        traceEvents.add(createTraceEvent(catchDate.plusDays(1).toString() + " 09:15", "港口卸货入库", "鲜度等级：特A级"));
        traceEvents.add(createTraceEvent(product.getListDate().toString() + " 02:00", "全程冷链运输中", "库温：-18.5°C"));
        traceEvents.add(createTraceEvent(product.getListDate().toString() + " 08:00", "到达城市前置仓", "已上架"));
        result.put("traceEvents", traceEvents);

        String origin = product.getOrigin() != null ? product.getOrigin() : "";
        double[] port = PORT_COORDINATES.get("DEFAULT");

        for (String key : PORT_COORDINATES.keySet()) {
            if (origin.contains(key)) {
                port = PORT_COORDINATES.get(key);
                break;
            }
        }

        boolean isImport = origin.contains("进口") || origin.contains("大西洋") || origin.contains("远洋") || origin.contains("美洲");

        List<double[]> trajectory = new ArrayList<>();
        double endLng = port[0];
        double endLat = port[1];
        double startLng, startLat;

        if (isImport) {
            startLng = endLng + 15.0 + random.nextDouble() * 5.0; 
            startLat = endLat - 10.0 + random.nextDouble() * 5.0; 
        } else {
            startLng = endLng + 3.0 + random.nextDouble() * 2.0;
            startLat = endLat + (random.nextDouble() - 0.5) * 4.0;
        }

        int steps = 40;
        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            
            double curveIntensity = isImport ? 2.5 : 0.5;
            double curve = Math.sin(ratio * Math.PI) * curveIntensity;
            
            double lng = startLng + (endLng - startLng) * ratio - curve;
            double lat = startLat + (endLat - startLat) * ratio + (random.nextDouble() - 0.5) * 0.05;
            
            trajectory.add(new double[]{lng, lat});
        }
        result.put("trajectory", trajectory);

        Map<String, Object> environment = new HashMap<>();
        environment.put("waterTemp", String.format("%.1f", 16.0 + random.nextDouble() * 4));
        environment.put("salinity", String.format("%.1f", 3.2 + random.nextDouble() * 0.3));
        environment.put("windSpeed", String.format("%.1f", 2.0 + random.nextDouble() * 5));
        environment.put("weather", random.nextBoolean() ? "晴朗" : "多云");
        result.put("environment", environment);

        result.put("blockchainHash",
                "0x" + Long.toHexString(Double.doubleToLongBits(Math.random())).toUpperCase() + "...VERIFIED");

        return result;
    }

    @DeleteMapping("/order/{id}")
    public Map<String, String> deleteOrder(@PathVariable Long id) {
        orderRepository.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "订单已删除");
        return response;
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
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("username");
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        orderService.createOrder(username, items);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "下单成功");
        return response;
    }

    @PostMapping("/order/{id}/receive")
    @Transactional
    public ResponseEntity<?> confirmReceipt(@PathVariable Long id) {
        OrderRecord order = orderRepository.findById(id).orElse(null);
        if (order == null)
            return ResponseEntity.badRequest().body("订单不存在");
        if ("已送达".equals(order.getStatus()))
            return ResponseEntity.badRequest().body("订单已完成");
        order.setStatus("已送达");
        orderRepository.save(order);
        User user = userRepository.findByUsername(order.getUsername());
        if (user != null) {
            if (user.getPoints() == null)
                user.setPoints(0);
            user.setPoints(user.getPoints() + order.getTotalPrice().intValue());
            userRepository.save(user);
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.ok("操作成功");
    }

    @GetMapping("/orders")
    public List<OrderRecord> getMyOrders(@RequestParam String username) {
        return orderRepository.findByUsernameOrderByCreateTimeDesc(username);
    }

    @GetMapping("/recommend")
    public List<Product> getDailyRecommendations() {
        return productRepository.findRandomRecommendations();
    }
}