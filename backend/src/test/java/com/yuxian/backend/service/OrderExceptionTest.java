package com.yuxian.backend.service;

import com.yuxian.backend.entity.Address;
import com.yuxian.backend.entity.Coupon;
import com.yuxian.backend.entity.UserCoupon;
import com.yuxian.backend.repository.UserCouponRepository;
import com.yuxian.backend.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderExceptionTest {

    @Mock
    private UserCouponRepository userCouponRepository;
    
    // 还需要 Mock ProductRepository 等，为了简洁省略 setup
    // 假设其他 Mock 已在 @BeforeEach 配置好

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void testCreateOrder_WithExpiredCoupon() {
        // 模拟一个过期的优惠券
        Long couponId = 99L;
        UserCoupon expiredCoupon = new UserCoupon();
        expiredCoupon.setId(couponId);
        expiredCoupon.setStatus("USED"); // 或者是 EXPIRED
        expiredCoupon.setUsername("testUser");

        when(userCouponRepository.findById(couponId)).thenReturn(Optional.of(expiredCoupon));

        // 构造下单参数
        List<Map<String, Object>> items = new ArrayList<>(); // 假设填入了商品
        Address address = new Address();

        // 执行并断言抛出异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder("testUser", items, address, couponId);
        });

        // 验证错误信息
        assertTrue(exception.getMessage().contains("已使用或已过期"));
    }
    
    @Test
    void testCreateOrder_CouponThresholdNotMet() {
        // 模拟未达到满减金额
        // ... 设置商品总价 50，优惠券门槛 100
        // 断言抛出 "未满足优惠券使用门槛"
    }
}