package com.yuxian.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        e.printStackTrace();

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("success", false);
        body.put("status", 400);
        body.put("message", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        e.printStackTrace();

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("success", false);
        body.put("status", 500);
        body.put("message", "服务器繁忙，请稍后重试");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(org.springframework.transaction.TransactionSystemException.class)
    public ResponseEntity<Map<String, Object>> handleTxException(org.springframework.transaction.TransactionSystemException e) {
        // 尝试获取最底层的异常原因
        Throwable rootCause = e.getRootCause();
        String message = (rootCause != null) ? rootCause.getMessage() : e.getMessage();

        // 打印到控制台方便开发查看
        e.printStackTrace();

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("success", false);
        body.put("status", 400);
        // 如果是乐观锁异常，提示得更友好一点
        if (message != null && message.contains("Row was updated or deleted by another transaction")) {
            body.put("message", "数据版本冲突，请刷新后重试");
        } else {
            body.put("message", "数据库错误: " + message);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}