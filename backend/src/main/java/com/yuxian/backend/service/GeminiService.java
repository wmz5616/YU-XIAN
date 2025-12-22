package com.yuxian.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    public String getRecipeAdvice(String productName, String userQuestion) {
        // 拼接 API Key
        String fullUrl = apiUrl + apiKey;

        // 🟢 关键修改：配置本地代理 (解决 Google 连不上的问题)
        // 如果你的 VPN 端口不是 7890，请在这里修改！
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));
        factory.setProxy(proxy);
        factory.setConnectTimeout(15000); // 设置超时时间 15秒
        factory.setReadTimeout(15000);

        RestTemplate restTemplate = new RestTemplate(factory);

        // 构建提示词
        String systemPrompt = "你是一位精通海鲜烹饪的米其林大厨。用户正在询问关于产品【" + productName + "】的问题。请用简洁、诱人且专业的语言回答。不要废话。";
        
        // 构建请求体
        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", List.of(Map.of("text", systemPrompt + "\n用户问题：" + userQuestion)));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            System.out.println("正在请求 Gemini API: " + fullUrl); // 打印日志方便调试
            Map response = restTemplate.postForObject(fullUrl, request, Map.class);
            
            // 解析结果
            if (response != null && response.containsKey("candidates")) {
                List<Map> candidates = (List<Map>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map contentPart = (Map) candidates.get(0).get("content");
                    List<Map> parts = (List<Map>) contentPart.get("parts");
                    return (String) parts.get(0).get("text");
                }
            }
            return "大厨正在思考，但好像没有说话...";

        } catch (Exception e) {
            e.printStackTrace(); // 🔴 这一步非常重要，看控制台报错是什么
            return "连接大厨失败，请检查后端控制台日志 (Proxy/VPN error?)";
        }
    }
}