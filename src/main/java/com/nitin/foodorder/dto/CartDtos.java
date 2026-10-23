package com.nitin.foodorder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddCartItemRequest(
            @NotNull Long menuItemId,
            @Min(1) @Max(50) int quantity) {
    }

    public record QuantityRequest(@Min(1) @Max(50) int quantity) {
    }

    public record CartItemResponse(Long menuItemId, String name, BigDecimal unitPrice, int quantity,
                                   BigDecimal lineTotal) {
    }

    public record CartResponse(Long restaurantId, List<CartItemResponse> items, BigDecimal total) {
    }
}
