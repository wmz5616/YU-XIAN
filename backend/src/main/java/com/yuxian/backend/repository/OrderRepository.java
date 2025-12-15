package com.yuxian.backend.repository;

import com.yuxian.backend.entity.OrderRecord;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
// import java.util.List; // 不再需要 List

public interface OrderRepository extends JpaRepository<OrderRecord, Long> {
    // 原有的前台查询（保持不变，或者也加上 Pageable）
    // List<OrderRecord> findByUsernameOrderByCreateTimeDesc(String username);
    
    // ✅ 新增：后台带搜索的分页查询
    // 逻辑：搜索用户名包含 keyword 或者 订单ID等于 keyword (转成Long有点麻烦，先只搜用户名)
    Page<OrderRecord> findByUsernameContainingOrderByCreateTimeDesc(String username, Pageable pageable);

    List<OrderRecord> findByUsernameOrderByCreateTimeDesc(String username);
}