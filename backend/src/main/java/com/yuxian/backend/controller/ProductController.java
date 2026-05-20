package com.yuxian.backend.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.yuxian.backend.entity.OrderRecord;
import com.yuxian.backend.entity.Product;
import com.yuxian.backend.entity.RestockReminder;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.ProductRepository;
import com.yuxian.backend.repository.RestockReminderRepository;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import com.yuxian.backend.service.OrderService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final com.yuxian.backend.repository.OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestockReminderRepository restockReminderRepository;

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
            RestockReminderRepository restockReminderRepository,
            OrderService orderService) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.restockReminderRepository = restockReminderRepository;
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

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        product.setId(null);
        if (product.getListDate() == null) {
            product.setListDate(LocalDate.now());
        }
        Product saved = productRepository.save(product);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        if (product.getName() != null) existing.setName(product.getName());
        if (product.getPrice() != null) existing.setPrice(product.getPrice());
        if (product.getStock() != null) existing.setStock(product.getStock());
        if (product.getImageUrl() != null) existing.setImageUrl(product.getImageUrl());
        if (product.getDescription() != null) existing.setDescription(product.getDescription());
        if (product.getCategory() != null) existing.setCategory(product.getCategory());
        if (product.getOrigin() != null) existing.setOrigin(product.getOrigin());

        Product saved = productRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        restockReminderRepository.deleteAll(
                restockReminderRepository.findByStatusOrderByCreateTimeDesc("PENDING").stream()
                        .filter(r -> r.getProductId().equals(id))
                        .toList());
        productRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "商品已下架"));
    }

    @GetMapping("/{id}/insight")
    public Map<String, Object> getProductInsight(@PathVariable Long id) {
        Product product = productRepository.findById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();

        if (product == null)
            return result;

        List<Map<String, Object>> priceHistory = new ArrayList<>();

        double currentPrice = product.getPrice().doubleValue();

        LocalDate today = LocalDate.now();
        Random random = new Random(id);

        List<Double> simulatedPrices = new ArrayList<>();
        simulatedPrices.add(currentPrice);

        double tempPrice = currentPrice;
        for (int i = 0; i < 6; i++) {
            double volatility = 0.03;
            double change = 1.0 + (random.nextGaussian() * volatility);
            tempPrice = tempPrice / change;

            if (tempPrice < currentPrice * 0.5)
                tempPrice = currentPrice * 0.55;
            if (tempPrice > currentPrice * 1.5)
                tempPrice = currentPrice * 1.45;

            simulatedPrices.add(tempPrice);
        }

        Collections.reverse(simulatedPrices);

        for (int i = 0; i < 7; i++) {
            Map<String, Object> point = new HashMap<>();
            LocalDate date = today.minusDays(6 - i);
            double p = simulatedPrices.get(i);
            point.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
            point.put("price", Double.parseDouble(String.format("%.2f", p)));
            priceHistory.add(point);
        }
        result.put("priceHistory", priceHistory);

        List<Map<String, Object>> traceEvents = new ArrayList<>();
        LocalDate catchDate = today.minusDays(2);
        LocalDate transitDate = today.minusDays(1);
        traceEvents.add(
                createTraceEvent(catchDate.toString() + " 04:30", "捕捞作业完成", "作业渔船：浙普渔" + (60000 + id * 123) + "号"));
        traceEvents.add(createTraceEvent(transitDate.toString() + " 09:15", "港口卸货入库", "鲜度等级：特A级"));
        traceEvents.add(createTraceEvent(today.toString() + " 02:00", "全程冷链运输中", "库温：-18.5°C"));
        traceEvents.add(createTraceEvent(today.toString() + " 08:00", "到达城市前置仓", "已上架"));
        result.put("traceEvents", traceEvents);

        String origin = product.getOrigin() != null ? product.getOrigin() : "";
        double[] port = PORT_COORDINATES.get("DEFAULT");

        for (String key : PORT_COORDINATES.keySet()) {
            if (origin.contains(key)) {
                port = PORT_COORDINATES.get(key);
                break;
            }
        }

        boolean isImport = origin.contains("进口") || origin.contains("大西洋") || origin.contains("远洋")
                || origin.contains("美洲");

        List<double[]> trajectory = new ArrayList<>();
        double endLng = port[0];
        double endLat = port[1];
        double startLng, startLat;
        Random rnd = new Random();

        if (isImport) {
            startLng = endLng + 15.0 + rnd.nextDouble() * 5.0;
            startLat = endLat - 10.0 + rnd.nextDouble() * 5.0;
        } else {
            startLng = endLng + 3.0 + rnd.nextDouble() * 2.0;
            startLat = endLat + (rnd.nextDouble() - 0.5) * 4.0;
        }

        int steps = 40;
        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            double curveIntensity = isImport ? 2.5 : 0.5;
            double curve = Math.sin(ratio * Math.PI) * curveIntensity;
            double lng = startLng + (endLng - startLng) * ratio - curve;
            double lat = startLat + (endLat - startLat) * ratio + (rnd.nextDouble() - 0.5) * 0.05;
            trajectory.add(new double[] { lng, lat });
        }
        result.put("trajectory", trajectory);

        Map<String, Object> environment = new HashMap<>();
        environment.put("waterTemp", String.format("%.1f", 16.0 + rnd.nextDouble() * 4));
        environment.put("salinity", String.format("%.1f", 3.2 + rnd.nextDouble() * 0.3));
        environment.put("windSpeed", String.format("%.1f", 2.0 + rnd.nextDouble() * 5));
        environment.put("weather", rnd.nextBoolean() ? "晴朗" : "多云");
        result.put("environment", environment);

        result.put("blockchainHash",
                "0x" + Long.toHexString(Double.doubleToLongBits(Math.random())).toUpperCase() + "...VERIFIED");

        return result;
    }

    @DeleteMapping("/order/{id}")
    public Map<String, String> deleteOrder(@PathVariable Long id) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        OrderRecord order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!order.getUsername().equals(username)) {
            throw new RuntimeException("无权删除此订单");
        }

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

    @PostMapping("/order/{id}/receive")
    @Transactional
    public ResponseEntity<?> confirmReceipt(@PathVariable Long id) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        OrderRecord order = orderRepository.findById(id).orElse(null);
        if (order == null)
            return ResponseEntity.badRequest().body("订单不存在");

        if (!order.getUsername().equals(username)) {
            return ResponseEntity.status(403).body("无权操作此订单");
        }

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

    @GetMapping("/stock-overview")
    public ResponseEntity<?> getStockOverview() {
        List<Product> allProducts = productRepository.findAll();

        Map<String, String> categoryIcons = new LinkedHashMap<>();
        categoryIcons.put("鱼类", "🐟");
        categoryIcons.put("虾类", "🦐");
        categoryIcons.put("蟹类", "🦀");
        categoryIcons.put("贝类", "🐚");
        categoryIcons.put("头足类", "🦑");

        Map<Long, Long> reminderCounts = new HashMap<>();
        List<Object[]> rawCounts = restockReminderRepository.countPendingGroupByProduct();
        for (Object[] row : rawCounts) {
            reminderCounts.put((Long) row[0], (Long) row[1]);
        }

        List<Map<String, Object>> categories = new ArrayList<>();
        Map<String, List<Product>> grouped = allProducts.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCategory() != null ? p.getCategory() : "其他",
                        LinkedHashMap::new,
                        Collectors.toList()));

        int totalStock = 0;
        int lowStockCount = 0;
        int outOfStockCount = 0;

        for (Map.Entry<String, List<Product>> entry : grouped.entrySet()) {
            String cat = entry.getKey();
            List<Product> products = entry.getValue();

            Map<String, Object> catInfo = new HashMap<>();
            catInfo.put("name", cat);
            catInfo.put("icon", categoryIcons.getOrDefault(cat, "📦"));
            catInfo.put("productCount", products.size());

            int catTotalStock = 0;
            int catLowStock = 0;
            int catOutOfStock = 0;

            List<Map<String, Object>> productItems = new ArrayList<>();
            for (Product p : products) {
                int stock = p.getStock() != null ? p.getStock() : 0;
                catTotalStock += stock;
                if (stock == 0) catOutOfStock++;
                else if (stock < 20) catLowStock++;

                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("name", p.getName());
                item.put("stock", stock);
                item.put("price", p.getPrice());
                item.put("imageUrl", p.getImageUrl());
                item.put("origin", p.getOrigin());
                item.put("reminderCount", reminderCounts.getOrDefault(p.getId(), 0L));

                String stockStatus;
                if (stock == 0) stockStatus = "OUT";
                else if (stock < 20) stockStatus = "LOW";
                else stockStatus = "NORMAL";
                item.put("stockStatus", stockStatus);

                productItems.add(item);
            }

            productItems.sort((a, b) -> Integer.compare(
                    (int) a.get("stock"), (int) b.get("stock")));

            catInfo.put("totalStock", catTotalStock);
            catInfo.put("lowStockCount", catLowStock);
            catInfo.put("outOfStockCount", catOutOfStock);
            catInfo.put("products", productItems);
            categories.add(catInfo);

            totalStock += catTotalStock;
            lowStockCount += catLowStock;
            outOfStockCount += catOutOfStock;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("categories", categories);
        result.put("totalProducts", allProducts.size());
        result.put("totalStock", totalStock);
        result.put("lowStockCount", lowStockCount);
        result.put("outOfStockCount", outOfStockCount);
        result.put("pendingReminders", restockReminderRepository.findByStatusOrderByCreateTimeDesc("PENDING").size());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/restock-remind")
    public ResponseEntity<?> submitRestockReminder(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "商品不存在"));
        }

        boolean alreadyReminded = restockReminderRepository
                .existsByProductIdAndUsernameAndStatus(id, username, "PENDING");
        if (alreadyReminded) {
            return ResponseEntity.badRequest().body(Map.of("message", "您已提交过该商品的补货提醒，商家已收到通知"));
        }

        RestockReminder reminder = new RestockReminder();
        reminder.setProductId(id);
        reminder.setProductName(product.getName());
        reminder.setUsername(username);
        reminder.setCategory(product.getCategory());
        reminder.setCurrentStock(product.getStock());
        restockReminderRepository.save(reminder);

        long totalReminders = restockReminderRepository.countByProductIdAndStatus(id, "PENDING");

        try {
            com.yuxian.backend.service.WebSocketServer.sendInfo("RESTOCK_REMIND");
        } catch (Exception e) {
            System.err.println("WebSocket推送补货提醒失败: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "补货提醒已发送给商家！",
                "totalReminders", totalReminders));
    }

    @GetMapping("/{id}/reminder-count")
    public ResponseEntity<?> getReminderCount(@PathVariable Long id) {
        long count = restockReminderRepository.countByProductIdAndStatus(id, "PENDING");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean hasReminded = restockReminderRepository
                .existsByProductIdAndUsernameAndStatus(id, username, "PENDING");

        return ResponseEntity.ok(Map.of(
                "count", count,
                "hasReminded", hasReminded));
    }

    @GetMapping("/admin/restock-reminders")
    public ResponseEntity<?> getRestockReminders() {
        List<RestockReminder> reminders = restockReminderRepository.findAllByOrderByCreateTimeDesc();

        Map<Long, Long> pendingCounts = new HashMap<>();
        List<Object[]> rawCounts = restockReminderRepository.countPendingGroupByProduct();
        for (Object[] row : rawCounts) {
            pendingCounts.put((Long) row[0], (Long) row[1]);
        }

        Map<Long, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (RestockReminder r : reminders) {
            if (!"PENDING".equals(r.getStatus())) continue;
            grouped.computeIfAbsent(r.getProductId(), k -> new ArrayList<>()).add(Map.of(
                    "id", r.getId(),
                    "username", r.getUsername(),
                    "currentStock", r.getCurrentStock(),
                    "createTime", r.getCreateTime().toString()));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Map<String, Object>>> entry : grouped.entrySet()) {
            Long productId = entry.getKey();
            Product product = productRepository.findById(productId).orElse(null);

            Map<String, Object> item = new HashMap<>();
            item.put("productId", productId);
            item.put("productName", product != null ? product.getName() : "已下架");
            item.put("category", product != null ? product.getCategory() : "未知");
            item.put("currentStock", product != null ? product.getStock() : 0);
            item.put("imageUrl", product != null ? product.getImageUrl() : "");
            item.put("reminderCount", pendingCounts.getOrDefault(productId, 0L));
            item.put("reminders", entry.getValue());
            result.add(item);
        }

        result.sort((a, b) -> Long.compare(
                (long) b.get("reminderCount"), (long) a.get("reminderCount")));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/admin/restock-reminders/{productId}/resolve")
    @Transactional
    public ResponseEntity<?> resolveRestockReminders(@PathVariable Long productId) {
        List<RestockReminder> pending = restockReminderRepository.findByStatusOrderByCreateTimeDesc("PENDING")
                .stream()
                .filter(r -> r.getProductId().equals(productId))
                .collect(Collectors.toList());

        for (RestockReminder r : pending) {
            r.setStatus("RESOLVED");
            r.setResolveTime(LocalDateTime.now());
        }
        restockReminderRepository.saveAll(pending);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已处理 " + pending.size() + " 条补货提醒",
                "resolvedCount", pending.size()));
    }
}