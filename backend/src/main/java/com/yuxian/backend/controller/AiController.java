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
    public ResponseEntity<Map<String, String>> askChef(@RequestBody Map<String, String> payload) {
        String productName = payload.get("productName");
        String question = payload.get("question");

        String answer = geminiService.getRecipeAdvice(productName, question);

        return ResponseEntity.ok(Map.of("answer", answer));
    }
}