package com.nitin.foodorder.controller;

import com.nitin.foodorder.dto.CartDtos.AddCartItemRequest;
import com.nitin.foodorder.dto.CartDtos.CartResponse;
import com.nitin.foodorder.dto.CartDtos.QuantityRequest;
import com.nitin.foodorder.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(Principal principal) {
        return cartService.getCart(principal.getName());
    }

    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request, Principal principal) {
        return cartService.addItem(principal.getName(), request);
    }

    @PutMapping("/items/{menuItemId}")
    public CartResponse updateQuantity(@PathVariable Long menuItemId, @Valid @RequestBody QuantityRequest request,
                                       Principal principal) {
        return cartService.updateQuantity(principal.getName(), menuItemId, request.quantity());
    }

    @DeleteMapping("/items/{menuItemId}")
    public CartResponse removeItem(@PathVariable Long menuItemId, Principal principal) {
        return cartService.removeItem(principal.getName(), menuItemId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(Principal principal) {
        cartService.clear(principal.getName());
    }
}
