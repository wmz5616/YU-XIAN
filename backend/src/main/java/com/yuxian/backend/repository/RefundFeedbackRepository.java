package com.yuxian.backend.repository;

import com.yuxian.backend.entity.RefundFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RefundFeedbackRepository extends JpaRepository<RefundFeedback, Long> {

    List<RefundFeedback> findByOrderIdOrderByCreateTimeAsc(Long orderId);

}