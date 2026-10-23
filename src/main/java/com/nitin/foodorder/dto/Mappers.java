package com.nitin.foodorder.dto;

import com.nitin.foodorder.dto.CartDtos.CartItemResponse;
import com.nitin.foodorder.dto.OrderDtos.OrderItemResponse;
import com.nitin.foodorder.dto.OrderDtos.OrderResponse;
import com.nitin.foodorder.dto.RestaurantDtos.MenuItemResponse;
import com.nitin.foodorder.dto.RestaurantDtos.RestaurantResponse;
import com.nitin.foodorder.entity.CartItem;
import com.nitin.foodorder.entity.FoodOrder;
import com.nitin.foodorder.entity.MenuItem;
import com.nitin.foodorder.entity.OrderItem;
import com.nitin.foodorder.entity.Restaurant;

import java.math.BigDecimal;

/** Entity to DTO conversion. Call these inside a transaction because some associations are lazy. */
public final class Mappers {

    private Mappers() {
    }

    public static RestaurantResponse toResponse(Restaurant r) {
        return new RestaurantResponse(r.getId(), r.getName(), r.getDescription(), r.getCity(),
                r.getOwner().getName());
    }

    public static MenuItemResponse toResponse(MenuItem m) {
        return new MenuItemResponse(m.getId(), m.getRestaurant().getId(), m.getName(), m.getDescription(),
                m.getPrice(), m.isAvailable());
    }

    public static CartItemResponse toResponse(CartItem c) {
        MenuItem m = c.getMenuItem();
        return new CartItemResponse(m.getId(), m.getName(), m.getPrice(), c.getQuantity(),
                m.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
    }

    public static OrderItemResponse toResponse(OrderItem i) {
        return new OrderItemResponse(i.getMenuItemId(), i.getName(), i.getUnitPrice(), i.getQuantity(),
                i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
    }

    public static OrderResponse toResponse(FoodOrder o) {
        return new OrderResponse(o.getId(), o.getRestaurant().getId(), o.getRestaurant().getName(),
                o.getStatus(), o.getTotalAmount(), o.getDeliveryAddress(), o.getCreatedAt(),
                o.getItems().stream().map(Mappers::toResponse).toList());
    }
}
