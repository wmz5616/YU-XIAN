package com.yuxian.backend.component;

import com.yuxian.backend.entity.Product;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.ProductRepository;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInit(ProductRepository productRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static final Map<String, double[]> PRICE_RANGES = new HashMap<>();
    static {
        PRICE_RANGES.put("鲍鱼", new double[] { 5.0, 15.0 });
        PRICE_RANGES.put("生蚝", new double[] { 3.0, 8.0 });
        PRICE_RANGES.put("大闸蟹", new double[] { 35.0, 88.0 });
        PRICE_RANGES.put("波士顿龙虾", new double[] { 128.0, 198.0 });
        PRICE_RANGES.put("帝王蟹", new double[] { 800.0, 1500.0 });
        PRICE_RANGES.put("皮皮虾", new double[] { 45.0, 65.0 });
        PRICE_RANGES.put("DEFAULT", new double[] { 20.0, 60.0 });
    }

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() == 0) {
            System.out.println("正在初始化商品数据...");
            ClassPathResource resource = new ClassPathResource("data.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        createProduct(line);
                    }
                }
            } catch (Exception e) {
                System.err.println("读取文件失败: " + e.getMessage());
            }
        }

        if (userRepository.findByUsername("admin") == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setDisplayName("系统管理员");
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("管理员账号初始化完成：admin / admin123");
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
                p.setDescription(generateDescription(name, parts[2]));

                double[] range = PRICE_RANGES.getOrDefault(name.split("\\(")[0], PRICE_RANGES.get("DEFAULT"));
                double price = range[0] + Math.random() * (range[1] - range[0]);
                p.setPrice((double) Math.round(price * 100) / 100);
                p.setStock((int) (Math.random() * 50) + 10);

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
        if (productName.contains("北太平洋巨型章鱼"))
            return "/images/beitaipingyangjuxingzhangyu.jpg";
        if (productName.contains("三文鱼"))
            return "/images/sanwenyu.jpg";
        if (productName.contains("生蚝"))
            return "/images/shenghao.jpg";
        if (productName.contains("虾蛄"))
            return "/images/pipixia.jpg";
        if (productName.contains("曼氏无针乌贼"))
            return "/images/manshiwuzhenwuzei.jpg";
        if (productName.contains("波士顿龙虾"))
            return "/images/boshidunlongxia.jpg";
        if (productName.contains("南美白对虾"))
            return "/images/nanmeibaiduixia.jpg";
        if (productName.contains("三疣梭子蟹"))
            return "/images/sanyousuozixie.jpg";
        if (productName.contains("远海梭子蟹"))
            return "/images/yuanhaisuozixie.jpg";
        if (productName.contains("短蛸"))
            return "/images/duanshao.jpg";
        if (productName.contains("剑尖枪乌贼"))
            return "/images/jianjianqiangwuzei.jpg";
        if (productName.contains("阿根廷鱿鱼"))
            return "/images/agentingyouyu.jpg";
        if (productName.contains("日本枪乌贼"))
            return "/images/ribenqiangwuzei.jpg";
        if (productName.contains("澳洲龙虾"))
            return "/images/aozhoulongxia.jpg";
        if (productName.contains("斑节对虾"))
            return "/images/banjieduixia.jpg";
        if (productName.contains("罗氏沼虾"))
            return "/images/luoshizhaoxia.jpg";
        if (productName.contains("北极甜虾"))
            return "/images/beijitianxia.jpg";
        if (productName.contains("潮汕青蟹"))
            return "/images/chaoshanqingxie.jpg";
        if (productName.contains("八腕总目"))
            return "/images/bawan.jpg";
        if (productName.contains("象拔蚌"))
            return "/images/xiangbabang.jpg";
        if (productName.contains("象牙蚌"))
            return "/images/xiangyabang.jpg";
        if (productName.contains("日月贝"))
            return "/images/riyuebei.jpg";
        if (productName.contains("石斑鱼"))
            return "/images/shibanyu.jpg";
        if (productName.contains("大黄鱼"))
            return "/images/dahuangyu.jpg";
        if (productName.contains("马鲛鱼"))
            return "/images/majiaoyu.jpg";
        if (productName.contains("小黄鱼"))
            return "/images/xiaohuangyu.jpg";
        if (productName.contains("多宝鱼"))
            return "/images/duobaoyu.jpg";
        if (productName.contains("多春鱼"))
            return "/images/duochunyu.jpg";
        if (productName.contains("金枪鱼"))
            return "/images/jinqiangyu.jpg";
        if (productName.contains("罗非鱼"))
            return "/images/luofeiyu.jpg";
        if (productName.contains("偏口鱼"))
            return "/images/piankouyu.jpg";
        if (productName.contains("武昌鱼"))
            return "/images/wuchangyu.jpg";
        if (productName.contains("金鲳鱼"))
            return "/images/jinchangyu.jpg";
        if (productName.contains("基围虾"))
            return "/images/jiweixia.jpg";
        if (productName.contains("小龙虾"))
            return "/images/xiaolongxia.jpg";
        if (productName.contains("竹节虾"))
            return "/images/zhujiexia.jpg";
        if (productName.contains("九节虾"))
            return "/images/jiujiexia.jpg";
        if (productName.contains("鹰爪虾"))
            return "/images/yingzhaoxia.jpg";
        if (productName.contains("大闸蟹"))
            return "/images/dazhaxie.jpg";
        if (productName.contains("梭子蟹"))
            return "/images/suozixie.jpg";
        if (productName.contains("帝王蟹"))
            return "/images/diwangxie.jpg";
        if (productName.contains("珍宝蟹"))
            return "/images/zhenbaoxie.jpg";
        if (productName.contains("面包蟹"))
            return "/images/mianbaoxie.jpg";
        if (productName.contains("红花蟹"))
            return "/images/honghuaxie.jpg";
        if (productName.contains("黄油蟹"))
            return "/images/huangyouxie.jpg";
        if (productName.contains("六月黄"))
            return "/images/liuyuehuang.jpg";
        if (productName.contains("兰花蟹"))
            return "/images/lanhuaxie.jpg";
        if (productName.contains("红毛蟹"))
            return "/images/hongmaoxie.jpg";
        if (productName.contains("晶莹蟹"))
            return "/images/jingyingxie.jpg";
        if (productName.contains("和乐蟹"))
            return "/images/helexie.jpg";
        if (productName.contains("笔管鱼"))
            return "/images/biguanyu.jpg";
        if (productName.contains("目斗鱼"))
            return "/images/mudouyu.jpg";
        if (productName.contains("鲍鱼"))
            return "/images/baoyu.jpg";
        if (productName.contains("扇贝"))
            return "/images/shanbei.jpg";
        if (productName.contains("花蛤"))
            return "/images/huage.jpg";
        if (productName.contains("海螺"))
            return "/images/hailuo.jpg";
        if (productName.contains("蛏子"))
            return "/images/chengzi.jpg";
        if (productName.contains("文蛤"))
            return "/images/wenge.jpg";
        if (productName.contains("青口"))
            return "/images/qingkou.jpg";
        if (productName.contains("田螺"))
            return "/images/tianluo.jpg";
        if (productName.contains("赤贝"))
            return "/images/chibei.jpg";
        if (productName.contains("带子"))
            return "/images/daizi.jpg";
        if (productName.contains("血蚶"))
            return "/images/xuehan.jpg";
        if (productName.contains("响螺"))
            return "/images/xiangluo.jpg";
        if (productName.contains("河蚌"))
            return "/images/hebang.jpg";
        if (productName.contains("泥螺"))
            return "/images/niluo.jpg";
        if (productName.contains("香螺"))
            return "/images/xiangluo2.jpg";
        if (productName.contains("鸟蛤"))
            return "/images/niaoge.jpg";
        if (productName.contains("带鱼"))
            return "/images/daiyu.jpg";
        if (productName.contains("鳕鱼"))
            return "/images/xueyu.jpg";
        if (productName.contains("鲈鱼"))
            return "/images/luyu.jpg";
        if (productName.contains("鲳鱼"))
            return "/images/changyu.jpg";
        if (productName.contains("草鱼"))
            return "/images/caoyu.jpg";
        if (productName.contains("鲤鱼"))
            return "/images/liyu.jpg";
        if (productName.contains("黑鱼"))
            return "/images/heiyu.jpg";
        if (productName.contains("鳗鱼"))
            return "/images/manyu.jpg";
        if (productName.contains("对虾"))
            return "/images/duixia.jpg";
        if (productName.contains("虎虾"))
            return "/images/huxia.jpg";
        if (productName.contains("明虾"))
            return "/images/mingxia.jpg";
        if (productName.contains("沼虾"))
            return "/images/zhaoxia.jpg";
        if (productName.contains("甜虾"))
            return "/images/tianxia.jpg";
        if (productName.contains("白虾"))
            return "/images/baixia.jpg";
        if (productName.contains("草虾"))
            return "/images/caoxia.jpg";
        if (productName.contains("红虾"))
            return "/images/hongxia.jpg";
        if (productName.contains("青蟹"))
            return "/images/qingxie.jpg";
        if (productName.contains("花蟹"))
            return "/images/huaxie.jpg";
        if (productName.contains("毛蟹"))
            return "/images/maoxie.jpg";
        if (productName.contains("紫蟹"))
            return "/images/zixie.jpg";
        if (productName.contains("石蟹"))
            return "/images/shixie.jpg";
        if (productName.contains("鱿鱼"))
            return "/images/youyu.jpg";
        if (productName.contains("墨鱼"))
            return "/images/moyu.jpg";
        if (productName.contains("章鱼"))
            return "/images/zhangyu.jpg";
        if (productName.contains("柔鱼"))
            return "/images/rouyu.jpg";
        if (productName.contains("花枝"))
            return "/images/huazhi.jpg";
        if (productName.contains("软丝"))
            return "/images/ruansi.jpg";
        if (productName.contains("小管"))
            return "/images/xiaoguan.jpg";
        if (productName.contains("透抽"))
            return "/images/touchou.jpg";
        if (productName.contains("真蛸"))
            return "/images/zhenshao.jpg";
        if (productName.contains("赤鱿"))
            return "/images/chiyou.jpg";
        if (productName.contains("长蛸"))
            return "/images/changshao.jpg";
        return "/images/default.jpg";
    }
}