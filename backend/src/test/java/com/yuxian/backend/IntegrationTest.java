package com.yuxian.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxian.backend.entity.Address;
import com.yuxian.backend.entity.Product;
import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.ProductRepository;
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
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "integrationUser", roles = { "USER" })
public class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Long testProductId;

    @BeforeEach
    void setup() {
        String username = "integrationUser";
        if (userRepository.findByUsername(username) == null) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setDisplayName("Integration Tester");
            user.setRole("USER");
            userRepository.save(user);
        }

        Product product = new Product();
        product.setName("集成测试商品");
        product.setPrice(new BigDecimal("99.00"));
        product.setStock(100);
        product.setCategory("test");
        product.setImageUrl("/test.jpg");
        product = productRepository.save(product);
        testProductId = product.getId();
    }

    @Test
    void testCreateOrderFlow() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("username", "integrationUser");

        Address address = new Address();
        address.setContact("Test Contact");
        address.setPhone("13812345678");
        address.setDetail("Test Address");
        request.put("address", address);

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("id", testProductId);
        item.put("quantity", 1);
        items.add(item);
        request.put("items", items);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
