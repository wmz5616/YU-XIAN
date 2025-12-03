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

@Component
public class DataInit implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataInit(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 每次启动时，如果发现数据库是空的，就从 data.txt 导入所有数据
        if (productRepository.count() == 0) {
            System.out.println("正在从 data.txt 批量导入海鲜数据...");
            
            // 读取 resources 目录下的 data.txt
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
                p.setName(parts[1]);
                p.setOrigin(parts[2]);
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                p.setListDate(LocalDate.parse(parts[3], formatter));
                p.setDescription(parts[4]);

                // 随机价格
                double randomPrice = 50.0 + Math.random() * 250.0;
                p.setPrice((double) Math.round(randomPrice * 100) / 100);

                // 随机库存
                int randomStock = (int)(Math.random() * 20) + 1;
                p.setStock(randomStock);

                // 【新增】图片映射逻辑 (在这里配置你的图片)
                // 逻辑：根据中文名称，指定对应的图片路径
                String imgPath = getImageMapping(p.getName());
                if (imgPath != null) {
                    p.setImageUrl(imgPath);
                }

                productRepository.save(p);
            }
        } catch (Exception e) {
            // 忽略错误行
        }
    }

    // 【新增】图片映射表 (你在这个方法里配置图片)
    private String getImageMapping(String productName) {
        // 这里可以使用 switch 或者 map
        // 只要你把图片放进 public/images/ 下，路径就是 "/images/文件名"
        switch (productName) {
            case "鲍鱼": return "/images/baoyu.jpg";
            case "大闸蟹": return "/images/dazhaxie.jpg";
            case "基围虾": return "/images/jiweixia.jpg";
            case "大黄鱼": return "/images/dahuangyu.jpg";
            case "帝王蟹": return "/images/diwangxie.jpg";
            case "波士顿龙虾": return "/images/longxia.jpg";
            // ... 你以后有新图了，就在这里加一行
            default: return null; // 没配置图片的，继续显示默认色块/纹理
        }

    }
}