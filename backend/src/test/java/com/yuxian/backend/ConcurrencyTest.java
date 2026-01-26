package com.yuxian.backend;

import com.yuxian.backend.entity.*;
import com.yuxian.backend.repository.*;
import com.yuxian.backend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConcurrencyTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final int THREAD_COUNT = 50;
    private static final String TEST_USER_PREFIX = "ct_user_";

    @BeforeEach
    void setup() {
        for (int i = 0; i < THREAD_COUNT; i++) {
            String username = TEST_USER_PREFIX + i;
            if (userRepository.findByUsername(username) == null) {
                User user = new User();
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode("123456"));
                user.setDisplayName("Test User " + i);
                user.setRole("USER");
                user.setPoints(1000);
                userRepository.save(user);
            }
        }
    }

    @Test
    void testConcurrentCouponReceive() throws InterruptedException {
        Coupon coupon = new Coupon();
        coupon.setName("并发测试优惠券");
        coupon.setAmount(new BigDecimal("10.00"));
        coupon.setMinSpend(new BigDecimal("0"));
        coupon.setTotalCount(10);
        coupon.setReceivedCount(0);
        coupon.setStatus(1);
        coupon.setValidUntil(LocalDate.now().plusDays(7));
        coupon = couponRepository.save(coupon);
        final Long couponId = coupon.getId();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    Boolean success = transactionTemplate.execute(status -> {
                        int rows = couponRepository.incrementReceivedCount(couponId);
                        if (rows > 0) {
                            UserCoupon uc = new UserCoupon();
                            uc.setUsername(TEST_USER_PREFIX + userId);
                            uc.setCouponId(couponId);
                            uc.setCouponName("并发测试优惠券");
                            uc.setAmount(new BigDecimal("10.00"));
                            uc.setMinSpend(new BigDecimal("0"));
                            uc.setReceiveTime(LocalDateTime.now());
                            uc.setStatus("UNUSED");
                            userCouponRepository.save(uc);
                            return true;
                        }
                        return false;
                    });

                    if (Boolean.TRUE.equals(success)) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Coupon finalCoupon = couponRepository.findById(couponId).orElseThrow();
        System.out.println("========================================");
        System.out.println("【优惠券并发抢购测试结果】");
        System.out.println("总量: 10, 实际领取: " + finalCoupon.getReceivedCount());
        System.out.println("成功数: " + successCount.get() + ", 失败数: " + failCount.get());
        System.out.println("========================================");

        assertTrue(finalCoupon.getReceivedCount() <= 10,
                "优惠券超发! 领取数: " + finalCoupon.getReceivedCount());
        assertEquals(10, successCount.get(), "成功领取数应该等于优惠券总量");
    }

    @Test
    void testConcurrentOrderCreate() throws InterruptedException {
        Product product = new Product();
        product.setName("并发测试商品");
        product.setPrice(new BigDecimal("99.00"));
        product.setStock(5);
        product.setCategory("test");
        product.setImageUrl("/test.jpg");
        product = productRepository.save(product);
        final Long productId = product.getId();

        int orderThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(orderThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(orderThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < orderThreads; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    Integer rows = transactionTemplate.execute(status -> productRepository.decreaseStock(productId, 1));
                    if (rows != null && rows > 0) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Product finalProduct = productRepository.findById(productId).orElseThrow();
        System.out.println("========================================");
        System.out.println("【商品库存并发扣减测试结果】");
        System.out.println("初始库存: 5, 剩余库存: " + finalProduct.getStock());
        System.out.println("下单成功: " + successCount.get() + ", 下单失败: " + failCount.get());
        System.out.println("========================================");

        assertTrue(finalProduct.getStock() >= 0, "库存变负! 当前: " + finalProduct.getStock());
        assertEquals(0, finalProduct.getStock(), "库存应该为0");
        assertEquals(5, successCount.get(), "成功下单数应该等于初始库存");
    }

    @Test
    void testConcurrentPointsExchange() throws InterruptedException {
        String testUsername = "points_test_user";
        User user = userRepository.findByUsername(testUsername);
        if (user == null) {
            user = new User();
            user.setUsername(testUsername);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setDisplayName("Points Test User");
            user.setRole("USER");
        }
        user.setPoints(500);
        userRepository.save(user);
        final Long userId = user.getId();

        int exchangeThreads = 10;
        int costPerExchange = 100;
        ExecutorService executor = Executors.newFixedThreadPool(exchangeThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(exchangeThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < exchangeThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    Integer rows = transactionTemplate
                            .execute(status -> userRepository.deductPoints(userId, costPerExchange));
                    if (rows != null && rows > 0) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        User finalUser = userRepository.findById(userId).orElseThrow();
        System.out.println("========================================");
        System.out.println("【积分并发兑换测试结果】");
        System.out.println("初始积分: 500, 剩余积分: " + finalUser.getPoints());
        System.out.println("兑换成功: " + successCount.get() + ", 兑换失败: " + failCount.get());
        System.out.println("========================================");

        assertTrue(finalUser.getPoints() >= 0,
                "积分变负! 当前: " + finalUser.getPoints() + " (存在并发漏洞!)");

        assertTrue(successCount.get() <= 5,
                "成功次数超过预期! 成功: " + successCount.get() + " (存在并发漏洞!)");
    }
}
