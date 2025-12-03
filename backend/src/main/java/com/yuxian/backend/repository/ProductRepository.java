package com.yuxian.backend.repository;

import com.yuxian.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // 原有的分类查询
    List<Product> findByCategory(String category);

    // 【新增】根据名称模糊查询 (Containing = SQL里的 LIKE %name%)
    List<Product> findByNameContaining(String name);
}