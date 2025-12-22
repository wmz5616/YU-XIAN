package com.yuxian.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxian.backend.entity.Address;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 测试结束后自动回滚数据库，保持环境干净
public class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setup() throws Exception {
        // 1. 初始化一个测试用户
        String username = "integrationUser";
        if (userRepository.findByUsername(username) == null) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setDisplayName("Integration Tester");
            user.setRole("USER");
            userRepository.save(user);
        }

        // 2. 模拟登录获取 Token (如果是 JWT 验证)
        // 这里简化处理，假设你有一个 Login 接口。
        // 如果无法模拟 Login，也可以使用 @WithMockUser(username="integrationUser") 注解跳过登录
    }

    @Test
    void testCreateOrderFlow() throws Exception {
        // 构造下单请求数据
        Map<String, Object> request = new HashMap<>();
        request.put("username", "integrationUser");
        
        Address address = new Address();
        address.setContact("Test Contact");
        address.setPhone("13812345678");
        address.setDetail("Test Address");
        request.put("address", address);

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("id", 1); // 假设数据库里 ID 1 的商品存在
        item.put("quantity", 1);
        items.add(item);
        request.put("items", items);

        // 发送下单请求
        mockMvc.perform(post("/api/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()); // 期望返回 200 OK
    }
}