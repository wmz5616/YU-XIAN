package com.yuxian.backend.repository;

import com.yuxian.backend.entity.WalletLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WalletLogRepository extends JpaRepository<WalletLog, Long> {
    List<WalletLog> findByUserIdOrderByCreateTimeDesc(Long userId);
}
