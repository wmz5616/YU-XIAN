package com.yuxian.backend.repository;

import com.yuxian.backend.entity.RefundFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RefundFeedbackRepository extends JpaRepository<RefundFeedback, Long> {
    
    // 关键查询方法：根据订单ID查询所有反馈，并按时间升序排列
    List<RefundFeedback> findByOrderIdOrderByCreateTimeAsc(Long orderId);
    
}