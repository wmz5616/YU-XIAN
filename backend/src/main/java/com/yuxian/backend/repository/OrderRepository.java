package com.yuxian.backend.repository;

import com.yuxian.backend.entity.OrderRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderRecord, Long> {

    List<OrderRecord> findByUsernameOrderByCreateTimeDesc(String username);

    Page<OrderRecord> findByUsernameContainingOrderByCreateTimeDesc(String username, Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0.00) FROM OrderRecord o")
    BigDecimal sumTotalSales();

    List<OrderRecord> findByCreateTimeAfter(LocalDateTime time);
}