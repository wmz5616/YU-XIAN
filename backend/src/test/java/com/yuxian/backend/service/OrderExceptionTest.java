package com.yuxian.backend.service;

import com.yuxian.backend.entity.Address;
import com.yuxian.backend.entity.Product;
import com.yuxian.backend.entity.UserCoupon;
import com.yuxian.backend.repository.OrderRepository;
import com.yuxian.backend.repository.ProductRepository;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderExceptionTest {

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void testCreateOrder_WithExpiredCoupon() {
        Long couponId = 99L;
        Long productId = 1L;

        UserCoupon expiredCoupon = new UserCoupon();
        expiredCoupon.setId(couponId);
        expiredCoupon.setStatus("USED");
        expiredCoupon.setUsername("testUser");

        Product product = new Product();
        product.setId(productId);
        product.setName("测试商品");
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(10);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.decreaseStock(anyLong(), anyInt())).thenReturn(1);
        when(userCouponRepository.findById(couponId)).thenReturn(Optional.of(expiredCoupon));

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("id", productId);
        item.put("quantity", 1);
        items.add(item);

        Address address = new Address();
        address.setContact("Test");
        address.setPhone("13800138000");
        address.setDetail("Test Address");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder("testUser", items, address, couponId);
        });

        assertTrue(exception.getMessage().contains("已使用或已过期"));
    }

    @Test
    void testCreateOrder_CouponThresholdNotMet() {
    }
}
