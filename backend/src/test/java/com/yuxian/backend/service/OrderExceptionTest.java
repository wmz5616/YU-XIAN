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

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void testCreateOrder_WithExpiredCoupon() {

        Long couponId = 99L;
        UserCoupon expiredCoupon = new UserCoupon();
        expiredCoupon.setId(couponId);
        expiredCoupon.setStatus("USED");
        expiredCoupon.setUsername("testUser");

        when(userCouponRepository.findById(couponId)).thenReturn(Optional.of(expiredCoupon));

        List<Map<String, Object>> items = new ArrayList<>();
        Address address = new Address();

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder("testUser", items, address, couponId);
        });

        assertTrue(exception.getMessage().contains("已使用或已过期"));
    }

    @Test
    void testCreateOrder_CouponThresholdNotMet() {
    }
}