package com.devops.shop.order;

import com.devops.shop.order.dto.OrderCreateRequest;
import com.devops.shop.order.dto.OrderItemDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Integration: GET /api/orders повертає 200 OK")
    void testListOrders() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Integration: POST /api/orders успішно оформлює замовлення (201 Created)")
    void testCreateOrderSuccess() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(
                "Максим Мельник",
                "maksym@test.com",
                "Київ, просп. Перемоги 37",
                List.of(
                        new OrderItemDto(1L, "Ноутбук Pro Developer", new BigDecimal("1899.99"), 1),
                        new OrderItemDto(2L, "Бездротова миша", new BigDecimal("69.99"), 2)
                )
        );

        // Expected total: 1899.99 + 139.98 = 2039.97
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.customerName", is("Максим Мельник")))
                .andExpect(jsonPath("$.totalAmount", is(2039.97)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @DisplayName("Integration: POST /api/orders з пустим списком товарів повертає 400 Bad Request")
    void testCreateOrderEmptyItems() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(
                "Максим",
                "maksym@test.com",
                "Київ",
                List.of()
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration: GET /actuator/health повертає UP")
    void testActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
