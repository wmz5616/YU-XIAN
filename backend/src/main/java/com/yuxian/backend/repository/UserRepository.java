package com.yuxian.backend.repository;

import com.yuxian.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);

    /**
     * 原子操作：扣减用户积分
     * 只有当用户积分足够时才会扣减，防止并发导致积分变负
     * 
     * @param id   用户ID
     * @param cost 扣减积分数
     * @return 更新行数，0表示积分不足或用户不存在
     */
    @Modifying
    @Query("UPDATE User u SET u.points = u.points - :cost WHERE u.id = :id AND u.points >= :cost")
    int deductPoints(@Param("id") Long id, @Param("cost") Integer cost);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.version = 0 WHERE u.version IS NULL")
    void fixNullVersions();
}