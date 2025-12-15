package com.yuxian.backend.repository;
import com.yuxian.backend.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    // 检查某人是否领过某张券
    boolean existsByUsernameAndCouponId(String username, Long couponId);
    
    // 查询某人的所有券
    List<UserCoupon> findByUsernameOrderByReceiveTimeDesc(String username);
}