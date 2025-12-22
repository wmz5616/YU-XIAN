package com.yuxian.backend.controller;

import com.yuxian.backend.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askAi(@RequestBody Map<String, String> payload) {

        String productName = payload.getOrDefault("productName", "");
        String question = payload.get("question");

        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("answer", "请先说出您的问题哦~"));
        }

        String answer = geminiService.getAiResponse(productName, question);

        return ResponseEntity.ok(Map.of("answer", answer));
    }
}