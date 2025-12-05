package com.yuxian.backend.repository;

import com.yuxian.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByCategory(String category);
    List<Product> findByNameContaining(String name);
}