package com.yuxian.backend.controller;

import com.yuxian.backend.entity.Coupon;
import com.yuxian.backend.entity.UserCoupon;
import com.yuxian.backend.repository.CouponRepository;
import com.yuxian.backend.repository.UserCouponRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public CouponController(CouponRepository couponRepository, UserCouponRepository userCouponRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
    }

    // 1. 领券中心：获取所有可领取的优惠券
    @GetMapping("/market")
    public List<Map<String, Object>> getMarketCoupons(@RequestParam String username) {
        List<Coupon> allCoupons = couponRepository.findByStatus(1);

        // ✅ 修复点：使用 HashMap 替代 Map.of，或者显式指定泛型，彻底解决 "Type mismatch" 问题
        return allCoupons.stream().map(coupon -> {
            boolean hasReceived = userCouponRepository.existsByUsernameAndCouponId(username, coupon.getId());

            // 使用 HashMap 更加稳健（允许空值，且类型明确）
            Map<String, Object> map = new HashMap<>();
            map.put("id", coupon.getId());
            map.put("name", coupon.getName());
            map.put("amount", coupon.getAmount());
            map.put("minSpend", coupon.getMinSpend());
            map.put("validUntil", coupon.getValidUntil());
            // 计算百分比
            double percent = 0.0;
            if (coupon.getTotalCount() > 0) {
                percent = (double) coupon.getReceivedCount() / coupon.getTotalCount();
            }
            map.put("percent", percent);
            map.put("hasReceived", hasReceived);

            return map;
        }).collect(Collectors.toList());
    }

    // 2. 我的优惠券
    @GetMapping("/my")
    public List<UserCoupon> getMyCoupons(@RequestParam String username) {
        return userCouponRepository.findByUsernameOrderByReceiveTimeDesc(username);
    }

    // 3. 领取优惠券 (核心事务逻辑)
    @PostMapping("/{id}/receive")
    @Transactional
    public Map<String, Object> receiveCoupon(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String username = payload.get("username");

        // 逻辑检查 A: 券是否存在
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("优惠券不存在"));

        // 逻辑检查 B: 库存是否足够
        if (coupon.getReceivedCount() >= coupon.getTotalCount()) {
            throw new RuntimeException("手慢了，优惠券已抢光");
        }

        // 逻辑检查 C: 是否已领取 (限领一张)
        if (userCouponRepository.existsByUsernameAndCouponId(username, id)) {
            throw new RuntimeException("您已经领取过该优惠券了");
        }

        // 执行领取
        // 1. 扣减库存 (增加领取数)
        coupon.setReceivedCount(coupon.getReceivedCount() + 1);
        couponRepository.save(coupon);

        // 2. 发放用户券
        UserCoupon uc = new UserCoupon();
        uc.setUsername(username);
        uc.setCouponId(coupon.getId());
        uc.setCouponName(coupon.getName());
        uc.setAmount(coupon.getAmount());
        uc.setMinSpend(coupon.getMinSpend());
        uc.setReceiveTime(LocalDateTime.now());
        uc.setStatus("UNUSED");
        userCouponRepository.save(uc);

        // 同样这里也用 HashMap 避免潜在的类型推断问题
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "领取成功");
        return result;
    }
}