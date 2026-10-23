package com.nitin.foodorder.controller;

import com.nitin.foodorder.dto.PageResponse;
import com.nitin.foodorder.dto.RestaurantDtos.MenuItemRequest;
import com.nitin.foodorder.dto.RestaurantDtos.MenuItemResponse;
import com.nitin.foodorder.dto.RestaurantDtos.RestaurantRequest;
import com.nitin.foodorder.dto.RestaurantDtos.RestaurantResponse;
import com.nitin.foodorder.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class RestaurantController {

    private static final Set<String> SORT_FIELDS = Set.of("name", "city", "id");

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping("/restaurants")
    public PageResponse<RestaurantResponse> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return PageResponse.from(restaurantService.search(city, query,
                PageUtil.of(page, size, sortBy, direction, SORT_FIELDS, "name")));
    }

    @GetMapping("/restaurants/{id}")
    public RestaurantResponse get(@PathVariable Long id) {
        return restaurantService.get(id);
    }

    @GetMapping("/restaurants/{id}/menu")
    public List<MenuItemResponse> menu(@PathVariable Long id) {
        return restaurantService.menu(id);
    }

    @PostMapping("/restaurants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public RestaurantResponse create(@Valid @RequestBody RestaurantRequest request, Principal principal) {
        return restaurantService.create(request, principal.getName());
    }

    @PutMapping("/restaurants/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public RestaurantResponse update(@PathVariable Long id, @Valid @RequestBody RestaurantRequest request,
                                     Principal principal) {
        return restaurantService.update(id, request, principal.getName());
    }

    @PostMapping("/restaurants/{id}/menu")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public MenuItemResponse addMenuItem(@PathVariable Long id, @Valid @RequestBody MenuItemRequest request,
                                        Principal principal) {
        return restaurantService.addMenuItem(id, request, principal.getName());
    }

    @PutMapping("/menu-items/{itemId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public MenuItemResponse updateMenuItem(@PathVariable Long itemId, @Valid @RequestBody MenuItemRequest request,
                                           Principal principal) {
        return restaurantService.updateMenuItem(itemId, request, principal.getName());
    }
}
