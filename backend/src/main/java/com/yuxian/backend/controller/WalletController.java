package com.yuxian.backend.controller;

import com.yuxian.backend.entity.WalletLog;
import com.yuxian.backend.repository.WalletLogRepository;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletLogRepository walletLogRepository;
    private final UserRepository userRepository;

    public WalletController(WalletLogRepository walletLogRepository, UserRepository userRepository) {
        this.walletLogRepository = walletLogRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/logs")
    public ResponseEntity<List<WalletLog>> getMyLogs() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long userId = userRepository.findByUsername(username).getId();

        List<WalletLog> logs = walletLogRepository.findByUserIdOrderByCreateTimeDesc(userId);
        return ResponseEntity.ok(logs);
    }
}
