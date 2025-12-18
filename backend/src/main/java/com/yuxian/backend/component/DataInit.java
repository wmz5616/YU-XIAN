package com.yuxian.backend.component;

import com.yuxian.backend.entity.Coupon;
import com.yuxian.backend.entity.Product;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.CouponRepository;
import com.yuxian.backend.repository.ProductRepository;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
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
    private final CouponRepository couponRepository;

    public DataInit(ProductRepository productRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CouponRepository couponRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.couponRepository = couponRepository;
    }

    private static final Map<String, double[]> PRICE_RANGES = new HashMap<>();
    static {
        PRICE_RANGES.put("带鱼", new double[] { 35.0, 58.0 });
        PRICE_RANGES.put("大黄鱼", new double[] { 45.0, 80.0 });
        PRICE_RANGES.put("小黄鱼", new double[] { 25.0, 40.0 });
        PRICE_RANGES.put("三文鱼", new double[] { 130.0, 180.0 });
        PRICE_RANGES.put("鳕鱼", new double[] { 150.0, 220.0 });
        PRICE_RANGES.put("石斑鱼", new double[] { 65.0, 110.0 });
        PRICE_RANGES.put("多宝鱼", new double[] { 50.0, 70.0 });
        PRICE_RANGES.put("金枪鱼", new double[] { 200.0, 400.0 });
        PRICE_RANGES.put("鳗鱼", new double[] { 70.0, 100.0 });
        PRICE_RANGES.put("鲈鱼", new double[] { 20.0, 35.0 });
        PRICE_RANGES.put("草鱼", new double[] { 12.0, 18.0 });
        PRICE_RANGES.put("鲤鱼", new double[] { 10.0, 16.0 });
        PRICE_RANGES.put("罗非鱼", new double[] { 12.0, 20.0 });
        PRICE_RANGES.put("鲳鱼", new double[] { 40.0, 75.0 });
        PRICE_RANGES.put("波士顿龙虾", new double[] { 220.0, 320.0 });
        PRICE_RANGES.put("澳洲龙虾", new double[] { 500.0, 800.0 });
        PRICE_RANGES.put("基围虾", new double[] { 55.0, 85.0 });
        PRICE_RANGES.put("皮皮虾", new double[] { 60.0, 90.0 });
        PRICE_RANGES.put("虾蛄", new double[] { 60.0, 90.0 });
        PRICE_RANGES.put("南美白对虾", new double[] { 40.0, 60.0 });
        PRICE_RANGES.put("罗氏沼虾", new double[] { 70.0, 100.0 });
        PRICE_RANGES.put("斑节对虾", new double[] { 110.0, 160.0 });
        PRICE_RANGES.put("北极甜虾", new double[] { 90.0, 130.0 });
        PRICE_RANGES.put("小龙虾", new double[] { 30.0, 50.0 });
        PRICE_RANGES.put("帝王蟹", new double[] { 450.0, 750.0 });
        PRICE_RANGES.put("珍宝蟹", new double[] { 180.0, 260.0 });
        PRICE_RANGES.put("面包蟹", new double[] { 120.0, 180.0 });
        PRICE_RANGES.put("大闸蟹", new double[] { 100.0, 200.0 });
        PRICE_RANGES.put("梭子蟹", new double[] { 80.0, 140.0 });
        PRICE_RANGES.put("青蟹", new double[] { 110.0, 160.0 });
        PRICE_RANGES.put("红毛蟹", new double[] { 350.0, 550.0 });
        PRICE_RANGES.put("鲍鱼", new double[] { 80.0, 150.0 });
        PRICE_RANGES.put("生蚝", new double[] { 20.0, 40.0 });
        PRICE_RANGES.put("象拔蚌", new double[] { 250.0, 400.0 });
        PRICE_RANGES.put("扇贝", new double[] { 30.0, 50.0 });
        PRICE_RANGES.put("花蛤", new double[] { 10.0, 18.0 });
        PRICE_RANGES.put("蛏子", new double[] { 25.0, 45.0 });
        PRICE_RANGES.put("鱿鱼", new double[] { 35.0, 55.0 });
        PRICE_RANGES.put("墨鱼", new double[] { 40.0, 65.0 });
        PRICE_RANGES.put("章鱼", new double[] { 45.0, 70.0 });
        PRICE_RANGES.put("DEFAULT", new double[] { 30.0, 60.0 });
    }

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() == 0) {
            System.out.println(">>> 正在初始化商品数据...");
            ClassPathResource resource = new ClassPathResource("data.txt");
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty())
                            createProduct(line);
                    }
                } catch (Exception e) {
                    System.err.println("!!! 读取 data.txt 失败: " + e.getMessage());
                }
            }
        }

        if (userRepository.findByUsername("admin") == null) {
            User admin = new User();
            admin.setUsername("admin");

            String initPassword = "123";
            admin.setPassword(passwordEncoder.encode(initPassword));

            admin.setDisplayName("超级管理员");
            admin.setRole("ADMIN");
            admin.setPoints(9999);
            admin.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=admin");
            userRepository.save(admin);
        }

        if (couponRepository.count() == 0) {
            createCoupon("新人见面礼", 15.0, 0.0, 100);
            createCoupon("满100减20", 20.0, 100.0, 50);
            createCoupon("海鲜狂欢节", 50.0, 300.0, 10);
            System.out.println(">>> 优惠券数据初始化完成");
        }
    }

    private void createCoupon(String name, Double amount, Double min, Integer total) {
        Coupon c = new Coupon();
        c.setName(name);
        c.setAmount(BigDecimal.valueOf(amount));
        c.setMinSpend(BigDecimal.valueOf(min));
        c.setTotalCount(total);
        c.setReceivedCount(0);
        c.setValidUntil(LocalDate.now().plusDays(30));
        c.setStatus(1);
        couponRepository.save(c);
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
                try {
                    p.setListDate(LocalDate.parse(parts[3], formatter));
                } catch (Exception e) {
                    p.setListDate(LocalDate.now());
                }
                p.setDescription(generateDescription(name, parts[2], parts[4]));

                String key = name.split("\\(")[0];
                double[] range = PRICE_RANGES.entrySet().stream()
                    .filter(entry -> key.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(PRICE_RANGES.getOrDefault("DEFAULT", new double[]{30.0, 60.0}));

                double randomPrice = range[0] + Math.random() * (range[1] - range[0]);
                double roundedPrice = Math.round(randomPrice * 100.0) / 100.0;

                p.setPrice(BigDecimal.valueOf(roundedPrice));
                
                p.setStock((int) (Math.random() * 190) + 10);

                String imgPath = getImageMapping(name);
                if (imgPath != null)
                    p.setImageUrl(imgPath);

                productRepository.save(p);
            }
        } catch (Exception e) {
            System.err.println("跳过错误数据行: " + line);
        }
    }

    private String generateDescription(String name, String origin, String originalDesc) {
        Random random = new Random();
        String[] marketing = {
                "全程冷链直达，锁住鲜活口感。",
                "核心产区直采，只选A级好货。",
                "深海慢养，肉质紧实Q弹。",
                "今日捕捞，极速发货，餐桌上的大海味道。"
        };
        String suffix = marketing[random.nextInt(marketing.length)];
        return String.format("【%s】%s %s", origin, originalDesc, suffix);
    }

    private String getImageMapping(String productName) {
        if (productName.contains("北太平洋巨型章鱼"))
            return "/images/beitaipingyangjuxingzhangyu.jpg";
        if (productName.contains("三文鱼"))
            return "/images/sanwenyu.jpg";
        if (productName.contains("生蚝"))
            return "/images/shenghao.jpg";
        if (productName.contains("虾蛄") || productName.contains("皮皮虾"))
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