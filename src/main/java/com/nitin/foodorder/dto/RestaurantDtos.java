package com.nitin.foodorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class RestaurantDtos {

    private RestaurantDtos() {
    }

    public record RestaurantRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 500) String description,
            @NotBlank @Size(max = 100) String city) {
    }

    public record RestaurantResponse(Long id, String name, String description, String city, String ownerName) {
    }

    /** available is optional on create (defaults to true) and on update (keeps current value). */
    public record MenuItemRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 500) String description,
            @NotNull @DecimalMin(value = "0.01", message = "must be at least 0.01") BigDecimal price,
            Boolean available) {
    }

    public record MenuItemResponse(Long id, Long restaurantId, String name, String description,
                                   BigDecimal price, boolean available) {
    }
}
