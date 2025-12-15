package com.yuxian.backend.repository;
import com.yuxian.backend.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    // 查询所有上架的优惠券
    List<Coupon> findByStatus(Integer status);
}