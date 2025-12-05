package com.yuxian.backend.repository;

import com.yuxian.backend.entity.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderRecord, Long> {
    List<OrderRecord> findByUsernameOrderByCreateTimeDesc(String username);
}