package com.nitin.foodorder.dto;

import com.nitin.foodorder.entity.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record PlaceOrderRequest(@NotBlank @Size(max = 255) String deliveryAddress) {
    }

    public record StatusUpdateRequest(@NotNull OrderStatus status) {
    }

    public record OrderItemResponse(Long menuItemId, String name, BigDecimal unitPrice, int quantity,
                                    BigDecimal lineTotal) {
    }

    public record OrderResponse(Long id, Long restaurantId, String restaurantName, OrderStatus status,
                                BigDecimal totalAmount, String deliveryAddress, Instant createdAt,
                                List<OrderItemResponse> items) {
    }
}
