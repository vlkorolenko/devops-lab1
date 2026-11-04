package com.devops.shop.product;

import com.devops.shop.product.dto.ProductCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Integration: GET /api/products повертає список товарів (200 OK)")
    void testListProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    @DisplayName("Integration: GET /api/products/categories повертає унікальні категорії")
    void testListCategories() throws Exception {
        mockMvc.perform(get("/api/products/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    @DisplayName("Integration: GET /api/products/{id} для неіснуючого товару повертає 404")
    void testGetNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Integration: POST /api/products успішно створює новий товар (201 Created)")
    void testCreateProductSuccess() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Тестовий смарт-годинник",
                "Водонепроникний смарт-годинник з пульсометром",
                new BigDecimal("199.99"),
                "Wearables",
                12,
                "https://example.com/watch.jpg"
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Тестовий смарт-годинник")))
                .andExpect(jsonPath("$.price", is(199.99)))
                .andExpect(jsonPath("$.category", is("Wearables")));
    }

    @Test
    @DisplayName("Integration: POST /api/products з некоректними даними повертає 400 Bad Request")
    void testCreateProductValidationFailure() throws Exception {
        // Empty name and negative price
        String invalidJson = """
                {
                    "name": "",
                    "price": -10.0,
                    "category": "",
                    "stock": -1
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration: GET /actuator/health повертає статус UP")
    void testActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
