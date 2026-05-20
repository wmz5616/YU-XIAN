package com.yuxian.backend.controller;

import com.yuxian.backend.entity.User;
import com.yuxian.backend.entity.WalletLog;
import com.yuxian.backend.repository.WalletLogRepository;
import com.yuxian.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    @PostMapping("/recharge")
    @Transactional
    public ResponseEntity<?> recharge(@RequestBody Map<String, Object> payload) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户不存在"));
        }

        Object amountObj = payload.get("amount");
        if (amountObj == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "充值金额不能为空"));
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "充值金额格式不正确"));
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "充值金额必须大于0"));
        }

        BigDecimal oldBalance = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        user.setBalance(oldBalance.add(amount));
        userRepository.save(user);

        WalletLog log = new WalletLog();
        log.setUserId(user.getId());
        log.setAmount(amount);
        log.setType(3);
        log.setDescription("钱包在线充值");
        walletLogRepository.save(log);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "充值成功",
            "balance", user.getBalance()
        ));
    }
}
