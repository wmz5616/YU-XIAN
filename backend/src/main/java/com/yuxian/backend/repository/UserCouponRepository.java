package com.yuxian.backend.repository;

import com.yuxian.backend.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    boolean existsByUsernameAndCouponId(String username, Long couponId);

    List<UserCoupon> findByUsernameOrderByReceiveTimeDesc(String username);
}