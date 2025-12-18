package com.yuxian.backend.controller;

import com.yuxian.backend.entity.Coupon;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.entity.UserCoupon;
import com.yuxian.backend.repository.CouponRepository;
import com.yuxian.backend.repository.UserCouponRepository;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    private final UserRepository userRepository;

    public CouponController(CouponRepository couponRepository,
            UserCouponRepository userCouponRepository,
            UserRepository userRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/market")
    public List<Map<String, Object>> getMarketCoupons(@RequestParam String username) {
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
    public List<UserCoupon> getMyCoupons(@RequestParam String username) {
        return userCouponRepository.findByUsernameOrderByReceiveTimeDesc(username);
    }

    @PostMapping("/{id}/receive")
    @Transactional
    public Map<String, Object> receiveCoupon(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("优惠券不存在"));

        if (coupon.getReceivedCount() >= coupon.getTotalCount()) {
            throw new RuntimeException("手慢了，优惠券已抢光");
        }
        if (userCouponRepository.existsByUsernameAndCouponId(username, id)) {
            throw new RuntimeException("您已经领取过该优惠券了");
        }

        coupon.setReceivedCount(coupon.getReceivedCount() + 1);
        couponRepository.save(coupon);

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
        String username = (String) payload.get("username");
        String name = (String) payload.get("name");

        double amountDouble = Double.parseDouble(payload.get("amount").toString());
        int cost = Integer.parseInt(payload.get("cost").toString());

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (user.getPoints() == null)
            user.setPoints(0);
        if (user.getPoints() < cost) {
            throw new RuntimeException("积分不足，无法兑换");
        }

        user.setPoints(user.getPoints() - cost);
        userRepository.save(user);

        UserCoupon uc = new UserCoupon();
        uc.setUsername(username);
        uc.setCouponId(-1L);
        uc.setCouponName(name);
        
        BigDecimal amountDecimal = BigDecimal.valueOf(amountDouble);
        uc.setAmount(amountDecimal);
        uc.setMinSpend(amountDecimal.multiply(BigDecimal.TEN));
        
        uc.setStatus("UNUSED");
        uc.setReceiveTime(LocalDateTime.now());

        userCouponRepository.save(uc);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("points", user.getPoints());
        result.put("message", "兑换成功");
        return result;
    }
}