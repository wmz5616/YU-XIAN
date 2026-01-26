package com.yuxian.backend.controller;

import com.yuxian.backend.entity.Coupon;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.entity.UserCoupon;
import com.yuxian.backend.repository.CouponRepository;
import com.yuxian.backend.repository.UserCouponRepository;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final com.yuxian.backend.repository.PointLogRepository pointLogRepository;

    public CouponController(CouponRepository couponRepository,
            UserCouponRepository userCouponRepository,
            UserRepository userRepository,
            com.yuxian.backend.repository.PointLogRepository pointLogRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.userRepository = userRepository;
        this.pointLogRepository = pointLogRepository;
    }

    private static final Map<Integer, ExchangeRule> EXCHANGE_RULES = new HashMap<>();
    static {
        EXCHANGE_RULES.put(101, new ExchangeRule(5.0, 500, "无门槛立减券", 0.0));
        EXCHANGE_RULES.put(102, new ExchangeRule(20.0, 1800, "满200可用", 200.0));
        EXCHANGE_RULES.put(103, new ExchangeRule(50.0, 4000, "海鲜盛宴专享", 500.0));
        EXCHANGE_RULES.put(104, new ExchangeRule(100.0, 8000, "至尊VIP礼券", 1000.0));
    }

    private static class ExchangeRule {
        double amount;
        int cost;
        String name;
        double minSpend;

        ExchangeRule(double amount, int cost, String name, double minSpend) {
            this.amount = amount;
            this.cost = cost;
            this.name = name;
            this.minSpend = minSpend;
        }
    }

    @GetMapping("/market")
    public List<Map<String, Object>> getMarketCoupons() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        List<Coupon> allCoupons = couponRepository.findByStatus(1);
        return allCoupons.stream().map(coupon -> {
            boolean hasReceived = userCouponRepository.existsByUsernameAndCouponId(username, coupon.getId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", coupon.getId());
            map.put("name", coupon.getName());
            map.put("amount", coupon.getAmount());
            map.put("minSpend", coupon.getMinSpend());
            map.put("validUntil", coupon.getValidUntil());
            double percent = 0.0;
            if (coupon.getTotalCount() > 0) {
                percent = (double) coupon.getReceivedCount() / coupon.getTotalCount();
            }
            map.put("percent", percent);
            map.put("hasReceived", hasReceived);
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/my")
    public List<UserCoupon> getMyCoupons() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userCouponRepository.findByUsernameOrderByReceiveTimeDesc(username);
    }

    @PostMapping("/{id}/receive")
    @Transactional
    public Map<String, Object> receiveCoupon(@PathVariable Long id) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("优惠券不存在"));

        if (coupon.getReceivedCount() >= coupon.getTotalCount()) {
            throw new RuntimeException("手慢了，优惠券已抢光");
        }
        if (userCouponRepository.existsByUsernameAndCouponId(username, id)) {
            throw new RuntimeException("您已经领取过该优惠券了");
        }

        int updatedRows = couponRepository.incrementReceivedCount(id);
        if (updatedRows == 0) {
            throw new RuntimeException("手慢了，优惠券已抢光");
        }

        UserCoupon uc = new UserCoupon();
        uc.setUsername(username);
        uc.setCouponId(coupon.getId());
        uc.setCouponName(coupon.getName());
        uc.setAmount(coupon.getAmount());
        uc.setMinSpend(coupon.getMinSpend());
        uc.setReceiveTime(LocalDateTime.now());
        uc.setStatus("UNUSED");
        userCouponRepository.save(uc);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "领取成功");
        return result;
    }

    @PostMapping("/exchange")
    @Transactional
    public Map<String, Object> exchangeCoupon(@RequestBody Map<String, Object> payload) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Integer exchangeId = Integer.parseInt(payload.get("exchangeId").toString());

        ExchangeRule rule = EXCHANGE_RULES.get(exchangeId);
        if (rule == null) {
            throw new RuntimeException("无效的兑换请求");
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        int updatedRows = userRepository.deductPoints(user.getId(), rule.cost);
        if (updatedRows == 0) {
            throw new RuntimeException("积分不足，无法兑换");
        }

        user = userRepository.findById(user.getId()).orElseThrow();

        UserCoupon uc = new UserCoupon();
        uc.setUsername(username);
        uc.setCouponId(-1L);
        uc.setCouponName(rule.name);

        BigDecimal amountDecimal = BigDecimal.valueOf(rule.amount);
        uc.setAmount(amountDecimal);
        uc.setMinSpend(BigDecimal.valueOf(rule.minSpend));

        uc.setStatus("UNUSED");
        uc.setReceiveTime(LocalDateTime.now());

        userCouponRepository.save(uc);

        com.yuxian.backend.entity.PointLog log = new com.yuxian.backend.entity.PointLog();
        log.setUsername(username);
        log.setType(2);
        log.setAmount(rule.cost);
        log.setDescription("兑换: " + rule.name);
        pointLogRepository.save(log);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("points", user.getPoints());
        result.put("message", "兑换成功");
        return result;
    }
}