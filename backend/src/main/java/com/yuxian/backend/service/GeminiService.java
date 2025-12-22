package com.yuxian.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private static final boolean USE_PROXY = true;
    private static final String PROXY_HOST = "127.0.0.1";
    private static final int PROXY_PORT = 7897; 

    public String getAiResponse(String productName, String userQuestion) {
        String fullUrl = apiUrl + apiKey;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        if (USE_PROXY) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_HOST, PROXY_PORT));
            factory.setProxy(proxy);
        }
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(30000);
        factory.setBufferRequestBody(false); 

        RestTemplate restTemplate = new RestTemplate(factory);

        boolean shouldSearch = userQuestion.contains("视频") 
                            || userQuestion.toLowerCase().contains("video")
                            || userQuestion.contains("观看");

        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是一位海鲜领域的资深专家。");
        systemPrompt.append("请严格遵守以下规则：\n");
        systemPrompt.append("1. **直接回答**：不要废话，不要打招呼。\n");
        systemPrompt.append("2. **排版清晰**：使用Markdown格式（粗体、列表）。\n");

        if (shouldSearch) {
            systemPrompt.append("3. **联网搜索**：用户正在寻找【视频教程】，请务必使用 Google Search 工具找到 Bilibili/YouTube/下厨房 的视频链接。\n");
            systemPrompt.append("4. **链接格式**：必须以 Markdown 格式返回，例如：[点击观看视频](URL)。\n");
        } else {
            systemPrompt.append("3. **纯文本回答**：请根据你的知识库直接回答，无需联网搜索，除非你通过工具发现资料不足。\n");
        }
        
        if (StringUtils.hasText(productName)) {
            systemPrompt.append("\n用户当前正在查看：").append(productName);
        }

        String finalPrompt = systemPrompt.toString() + "\n\n用户问题：" + userQuestion;

        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", List.of(Map.of("text", finalPrompt)));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        if (shouldSearch) {
            System.out.println("🔍 检测到视频需求，正在挂载 Google Search 工具...");
            Map<String, Object> googleSearchTool = new HashMap<>();
            Map<String, Object> tool = new HashMap<>();
            tool.put("google_search", googleSearchTool);
            
            requestBody.put("tools", List.of(tool));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Connection", "close"); 

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            Map response = restTemplate.postForObject(fullUrl, request, Map.class);
            
            if (response != null && response.containsKey("candidates")) {
                List<Map> candidates = (List<Map>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map contentPart = (Map) candidates.get(0).get("content");
                    List<Map> parts = (List<Map>) contentPart.get("parts");
                    return (String) parts.get(0).get("text");
                }
            }
            return "（思考中...）";
        } catch (HttpClientErrorException e) {
            System.err.println("Gemini Error: " + e.getResponseBodyAsString());
            return "API Error: " + e.getStatusCode();
        } catch (Exception e) {
            e.printStackTrace();
            return "系统错误: " + e.getMessage();
        }
    }
}