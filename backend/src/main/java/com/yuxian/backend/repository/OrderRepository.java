package com.yuxian.backend.repository;

import com.yuxian.backend.entity.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderRecord, Long> {
    // 关键功能：找某个人的所有订单，按时间倒序排
    List<OrderRecord> findByUsernameOrderByCreateTimeDesc(String username);
}