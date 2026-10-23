package com.nitin.foodorder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end happy path and permission checks through the real security filter chain (H2 in MySQL mode). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FoodOrderFlowTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void customerOrdersFromAnOwnersRestaurantEndToEnd() throws Exception {
        String ownerToken = register("Olivia Owner", "owner@test.com", "RESTAURANT_OWNER");
        String customerToken = register("Chris Customer", "customer@test.com", "CUSTOMER");

        // Only owners may create restaurants.
        call(post("/api/restaurants"), customerToken,
                Map.of("name", "Nope", "description", "x", "city", "Delhi"), 403);

        JsonNode restaurant = call(post("/api/restaurants"), ownerToken,
                Map.of("name", "Spice Hub", "description", "North Indian", "city", "Delhi"), 201);
        long restaurantId = restaurant.get("id").asLong();

        JsonNode menuItem = call(post("/api/restaurants/" + restaurantId + "/menu"), ownerToken,
                Map.of("name", "Paneer Tikka", "description", "Starter", "price", 149.50, "available", true), 201);
        long menuItemId = menuItem.get("id").asLong();

        // Public browsing needs no token.
        JsonNode page = call(get("/api/restaurants?city=delhi"), null, null, 200);
        assertEquals(1, page.get("totalElements").asInt());

        // Cart.
        JsonNode cart = call(post("/api/cart/items"), customerToken,
                Map.of("menuItemId", menuItemId, "quantity", 2), 200);
        assertEquals(0, new BigDecimal("299.00").compareTo(cart.get("total").decimalValue()));

        // Order is created from the cart and the cart is emptied.
        JsonNode order = call(post("/api/orders"), customerToken, Map.of("deliveryAddress", "12 MG Road"), 201);
        long orderId = order.get("id").asLong();
        assertEquals("PLACED", order.get("status").asText());
        assertEquals(0, call(get("/api/cart"), customerToken, null, 200).get("items").size());

        // Status rules: customers cannot confirm, owners cannot skip steps.
        call(patch("/api/orders/" + orderId + "/status"), customerToken, Map.of("status", "CONFIRMED"), 403);
        call(patch("/api/orders/" + orderId + "/status"), ownerToken, Map.of("status", "DELIVERED"), 400);
        JsonNode confirmed = call(patch("/api/orders/" + orderId + "/status"), ownerToken,
                Map.of("status", "CONFIRMED"), 200);
        assertEquals("CONFIRMED", confirmed.get("status").asText());

        // Once confirmed, the customer can no longer cancel.
        call(patch("/api/orders/" + orderId + "/status"), customerToken, Map.of("status", "CANCELLED"), 400);

        // Authentication is required for protected routes.
        call(get("/api/cart"), null, null, 401);
    }

    private String register(String name, String email, String role) throws Exception {
        JsonNode response = call(post("/api/auth/register"), null,
                Map.of("name", name, "email", email, "password", "password123", "role", role), 201);
        return response.get("token").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus)
            throws Exception {
        request.contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.content(objectMapper.writeValueAsString(body));
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        MvcResult result = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        String content = result.getResponse().getContentAsString();
        return content.isEmpty() ? null : objectMapper.readTree(content);
    }
}
