package com.yuxian.backend.repository;

import com.yuxian.backend.entity.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // 新增导入
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderRecord, Long> {
    
    List<OrderRecord> findByUsernameOrderByCreateTimeDesc(String username);

    long countByStatus(String status);

    @Query("SELECT SUM(o.totalPrice) FROM OrderRecord o")
    Double selectTotalSales();
}