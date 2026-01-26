package com.yuxian.backend.repository;

import com.yuxian.backend.entity.PointLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PointLogRepository extends JpaRepository<PointLog, Long> {
    List<PointLog> findByUsernameOrderByCreateTimeDesc(String username);
}
