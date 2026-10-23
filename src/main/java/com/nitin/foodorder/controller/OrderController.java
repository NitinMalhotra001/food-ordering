package com.nitin.foodorder.controller;

import com.nitin.foodorder.dto.OrderDtos.OrderResponse;
import com.nitin.foodorder.dto.OrderDtos.PlaceOrderRequest;
import com.nitin.foodorder.dto.OrderDtos.StatusUpdateRequest;
import com.nitin.foodorder.dto.PageResponse;
import com.nitin.foodorder.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Set;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "totalAmount", "status", "id");

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse place(@Valid @RequestBody PlaceOrderRequest request, Principal principal) {
        return orderService.placeOrder(principal.getName(), request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PageResponse<OrderResponse> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Principal principal) {
        return PageResponse.from(orderService.myOrders(principal.getName(),
                PageUtil.of(page, size, sortBy, direction, SORT_FIELDS, "createdAt")));
    }

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public PageResponse<OrderResponse> restaurantOrders(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Principal principal) {
        return PageResponse.from(orderService.restaurantOrders(restaurantId, principal.getName(),
                PageUtil.of(page, size, sortBy, direction, SORT_FIELDS, "createdAt")));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, Principal principal) {
        return orderService.getOrder(id, principal.getName());
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request,
                                      Principal principal) {
        return orderService.updateStatus(id, principal.getName(), request.status());
    }
}
