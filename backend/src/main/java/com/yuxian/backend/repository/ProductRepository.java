package com.yuxian.backend.repository;

import com.yuxian.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByCategory(String category);
    List<Product> findByNameContaining(String name);

    @Query(value = "SELECT * FROM product ORDER BY RAND() LIMIT 4", nativeQuery = true)
    List<Product> findRandomRecommendations();

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    int decreaseStock(Long id, Integer quantity);

    // ✅ 新增方法：增加库存
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :id")
    void increaseStock(Long id, Integer quantity);
}