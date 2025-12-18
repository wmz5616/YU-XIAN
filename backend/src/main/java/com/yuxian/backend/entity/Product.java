package com.yuxian.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data 
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String category;
    private String name;
    private String origin;
    private LocalDate listDate;
    private String description;
    private BigDecimal price; 
    
    private Integer stock;
    private String imageUrl;
}