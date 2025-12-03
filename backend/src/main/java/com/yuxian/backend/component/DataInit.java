package com.yuxian.backend.component;

import com.yuxian.backend.entity.Product;
import com.yuxian.backend.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component
public class DataInit implements CommandLineRunner {

    private final ProductRepository productRepository;
    // 价格范围表 (单位: 元)
    private static final Map<String, double[]> PRICE_RANGES = new HashMap<>();
    
    static {
        // 配置各类商品的合理价格区间 [最低价, 最高价]
        PRICE_RANGES.put("鲍鱼", new double[]{5.0, 15.0}); // 5-15元/只
        PRICE_RANGES.put("生蚝", new double[]{3.0, 8.0});
        PRICE_RANGES.put("大闸蟹", new double[]{35.0, 88.0});
        PRICE_RANGES.put("波士顿龙虾", new double[]{128.0, 198.0});
        PRICE_RANGES.put("帝王蟹", new double[]{800.0, 1500.0});
        PRICE_RANGES.put("皮皮虾", new double[]{45.0, 65.0}); // 按斤算
        // 默认区间
        PRICE_RANGES.put("DEFAULT", new double[]{20.0, 60.0}); 
    }

    public DataInit(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() == 0) {
            System.out.println("正在从 data.txt 批量导入海鲜数据...");
            ClassPathResource resource = new ClassPathResource("data.txt");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        createProduct(line);
                        count++;
                    }
                }
                System.out.println("批量导入完成！共导入商品: " + count);
            } catch (Exception e) {
                System.err.println("读取文件失败: " + e.getMessage());
            }
        }
    }

    private void createProduct(String line) {
        try {
            line = line.replace("（", "(").replace("）", ")");
            String[] parts = line.split("-");
            if (parts.length >= 5) {
                Product p = new Product();
                p.setCategory(parts[0]);
                String name = parts[1];
                p.setName(name);
                p.setOrigin(parts[2]);
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                p.setListDate(LocalDate.parse(parts[3], formatter));

                // 1. 生成更生动的虚拟描述 (替换原来的 data.txt 里的简单描述)
                p.setDescription(generateDescription(name, parts[2]));

                // 2. 生成合理价格
                double[] range = PRICE_RANGES.getOrDefault(name.split("\\(")[0], PRICE_RANGES.get("DEFAULT"));
                double price = range[0] + Math.random() * (range[1] - range[0]);
                p.setPrice((double) Math.round(price * 100) / 100);

                // 3. 随机库存
                p.setStock((int)(Math.random() * 50) + 10);

                // 4. 图片映射
                String imgPath = getImageMapping(name);
                if (imgPath != null) {
                    p.setImageUrl(imgPath);
                }

                productRepository.save(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 生成生动描述的方法
    private String generateDescription(String name, String origin) {
        Random random = new Random();
        String[] templates = {
            origin + "核心海域纯净捕捞，%s肉质紧实，全程海水运输，不泡淡水拒绝增重。",
            "甄选" + origin + "优质%s，鲜活直达餐桌，口感鲜甜Q弹，每一口都是大海的味道。",
            "凌晨捕捞，当日发货，%s个大肥美，深海慢养2年以上，只有懂货的老饕才知道的美味。",
            "来自" + origin + "的馈赠，%s壳薄肉厚，富含优质蛋白，清蒸红烧皆是上品。"
        };
        String template = templates[random.nextInt(templates.length)];
        return String.format(template, name);
    }

    private String getImageMapping(String productName) {
        // 这里只是为了演示，保留之前的逻辑
        if (productName.contains("鲍鱼")) return "/images/baoyu.jpg";
        return null;
    }
}