package com.yuxian.backend.repository;

import com.yuxian.backend.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findByStatus(Integer status);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Coupon c SET c.receivedCount = c.receivedCount + 1 WHERE c.id = :id AND c.receivedCount < c.totalCount")
    int incrementReceivedCount(@org.springframework.data.repository.query.Param("id") Long id);
}