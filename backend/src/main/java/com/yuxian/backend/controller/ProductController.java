package com.yuxian.backend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.yuxian.backend.entity.OrderRecord;
import com.yuxian.backend.entity.Product;
import com.yuxian.backend.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@RestController // 告诉 Spring 这就是“服务员”
@RequestMapping("/api/products") // 设定服务窗口的地址
@CrossOrigin // 允许跨域（这很重要，否则以后前端网页访问会被拦截）
public class ProductController {

    private final ProductRepository productRepository;
    private final com.yuxian.backend.repository.OrderRepository orderRepository; // 新增

    // 修改构造函数，把 orderRepository 加进来
    public ProductController(ProductRepository productRepository, 
                             com.yuxian.backend.repository.OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }
    // 动作：获取所有商品
    // 访问地址：http://localhost:8080/api/products
    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // 动作：根据分类查询（比如只看“虾类”）
    // 访问地址：http://localhost:8080/api/products/category/虾类
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
        // 如果找不到这就返回 null (实际开发中会抛异常，这里简化处理)
        return productRepository.findById(id).orElse(null);
    }

    @GetMapping("/{id}/insight")
    public Map<String, Object> getProductInsight(@PathVariable Long id) {
        Product product = productRepository.findById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        
        if (product == null) return result;

        // --- 1. 生成“真实感”价格波动 (过去7天) ---
        List<Map<String, Object>> priceHistory = new ArrayList<>();
        double basePrice = product.getPrice();
        LocalDate today = LocalDate.now();
        Random random = new Random(id); // 使用ID作为种子，保证每次刷新看到的波动是一样的（稳定真实感）

        for (int i = 6; i >= 0; i--) {
            Map<String, Object> point = new HashMap<>();
            LocalDate date = today.minusDays(i);
            // 模拟波动：价格在基准价的 90% ~ 110% 之间浮动
            double factor = 0.9 + (random.nextDouble() * 0.2); 
            double dailyPrice = Math.round(basePrice * factor * 100.0) / 100.0;
            
            point.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
            point.put("price", dailyPrice);
            priceHistory.add(point);
        }
        result.put("priceHistory", priceHistory);

        // --- 2. 生成“硬核”溯源数据 ---
        List<Map<String, Object>> traceEvents = new ArrayList<>();
        
        // 模拟捕捞时间（上架前2天）
        LocalDate catchDate = product.getListDate().minusDays(2);
        
        // 节点1：捕捞
        traceEvents.add(createTraceEvent(
            catchDate.toString() + " 04:30", 
            "捕捞作业完成", 
            "作业渔船：浙普渔" + (60000 + id * 123) + "号 | 海域：东经122.5° 北纬30.2°"
        ));
        
        // 节点2：入库
        traceEvents.add(createTraceEvent(
            catchDate.plusDays(1).toString() + " 09:15", 
            "舟山港口卸货入库", 
            "检测批次：NO." + (20250000 + id) + " | 鲜度等级：特A级"
        ));
        
        // 节点3：冷链
        traceEvents.add(createTraceEvent(
            product.getListDate().toString() + " 02:00", 
            "全程冷链运输中", 
            "运输车牌：浙L·" + (8888 + id) + " | 实时库温：-18.5°C"
        ));

        // 节点4：上架
        traceEvents.add(createTraceEvent(
            product.getListDate().toString() + " 08:00", 
            "到达城市前置仓", 
            "已完成核酸检测与消杀，准备上架"
        ));

        result.put("traceEvents", traceEvents);
        
        // 模拟区块链哈希值（增加科技感）
        result.put("blockchainHash", "0x" + Long.toHexString(Double.doubleToLongBits(Math.random())).toUpperCase() + "...VERIFIED");

        return result;
    }

    private Map<String, Object> createTraceEvent(String time, String title, String desc) {
        Map<String, Object> event = new HashMap<>();
        event.put("time", time);
        event.put("title", title);
        event.put("desc", desc);
        return event;
    }

    @PostMapping("/order")
    @Transactional(rollbackFor = Exception.class) // 【关键】开启事务，出错自动回滚
    public String createOrder(@RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("username");
        
        // 解析前端传来的 items 数组: [{"id":1, "quantity":2}, ...]
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

        double total = 0.0;
        StringBuilder productNames = new StringBuilder();

        for (Map<String, Object> item : items) {
            Long pid = ((Number) item.get("id")).longValue();
            int quantity = ((Number) item.get("quantity")).intValue();

            // 1. 查商品
            Product product = productRepository.findById(pid)
                    .orElseThrow(() -> new RuntimeException("商品不存在: " + pid));

            // 2. 【关键】查库存
            if (product.getStock() < quantity) {
                throw new RuntimeException("商品 " + product.getName() + " 库存不足！剩余: " + product.getStock());
            }

            // 3. 【关键】扣库存
            product.setStock(product.getStock() - quantity);
            productRepository.save(product); // 更新到数据库

            // 4. 算钱
            total += product.getPrice() * quantity;
            productNames.append(product.getName()).append(" x").append(quantity).append(", ");
        }

        // 5. 生成订单
        OrderRecord order = new OrderRecord();
        order.setUsername(username);
        // 去掉字符串末尾多余的逗号
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

    // 【新增】查询某人的订单
    @GetMapping("/orders")
    public java.util.List<OrderRecord> getMyOrders(@RequestParam String username) {
        return orderRepository.findByUsernameOrderByCreateTimeDesc(username);
    }
}