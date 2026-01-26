package com.yuxian.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "point_logs")
public class PointLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private Integer type;

    private Integer amount;

    private String description;

    private LocalDateTime createTime;

    public PointLog() {
        this.createTime = LocalDateTime.now();
    }
}
