package com.yuxian.backend.repository;

import com.yuxian.backend.entity.RestockReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface RestockReminderRepository extends JpaRepository<RestockReminder, Long> {

    List<RestockReminder> findByStatusOrderByCreateTimeDesc(String status);

    List<RestockReminder> findAllByOrderByCreateTimeDesc();

    boolean existsByProductIdAndUsernameAndStatus(Long productId, String username, String status);

    long countByProductIdAndStatus(Long productId, String status);

    @Query("SELECT r.productId, COUNT(r) FROM RestockReminder r WHERE r.status = 'PENDING' GROUP BY r.productId ORDER BY COUNT(r) DESC")
    List<Object[]> countPendingGroupByProduct();
}
