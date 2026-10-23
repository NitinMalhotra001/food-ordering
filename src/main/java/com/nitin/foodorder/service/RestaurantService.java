package com.nitin.foodorder.service;

import com.nitin.foodorder.dto.Mappers;
import com.nitin.foodorder.dto.RestaurantDtos.MenuItemRequest;
import com.nitin.foodorder.dto.RestaurantDtos.MenuItemResponse;
import com.nitin.foodorder.dto.RestaurantDtos.RestaurantRequest;
import com.nitin.foodorder.dto.RestaurantDtos.RestaurantResponse;
import com.nitin.foodorder.entity.MenuItem;
import com.nitin.foodorder.entity.Restaurant;
import com.nitin.foodorder.entity.User;
import com.nitin.foodorder.exception.ResourceNotFoundException;
import com.nitin.foodorder.repository.MenuItemRepository;
import com.nitin.foodorder.repository.RestaurantRepository;
import com.nitin.foodorder.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;

    public RestaurantService(RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository,
                             UserRepository userRepository) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<RestaurantResponse> search(String city, String query, Pageable pageable) {
        return restaurantRepository.search(blankToEmpty(city), blankToEmpty(query), pageable)
                .map(Mappers::toResponse);
    }

    @Transactional(readOnly = true)
    public RestaurantResponse get(Long id) {
        return Mappers.toResponse(findRestaurant(id));
    }

    @Transactional
    public RestaurantResponse create(RestaurantRequest request, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Restaurant restaurant = new Restaurant();
        restaurant.setOwner(owner);
        apply(restaurant, request);
        return Mappers.toResponse(restaurantRepository.save(restaurant));
    }

    @Transactional
    public RestaurantResponse update(Long id, RestaurantRequest request, String ownerEmail) {
        Restaurant restaurant = findRestaurant(id);
        assertOwner(restaurant, ownerEmail);
        apply(restaurant, request);
        return Mappers.toResponse(restaurantRepository.save(restaurant));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> menu(Long restaurantId) {
        findRestaurant(restaurantId);
        return menuItemRepository.findByRestaurantIdOrderByNameAsc(restaurantId).stream()
                .map(Mappers::toResponse)
                .toList();
    }

    @Transactional
    public MenuItemResponse addMenuItem(Long restaurantId, MenuItemRequest request, String ownerEmail) {
        Restaurant restaurant = findRestaurant(restaurantId);
        assertOwner(restaurant, ownerEmail);
        MenuItem item = new MenuItem();
        item.setRestaurant(restaurant);
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setAvailable(request.available() == null || request.available());
        return Mappers.toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long itemId, MenuItemRequest request, String ownerEmail) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item " + itemId + " not found"));
        assertOwner(item.getRestaurant(), ownerEmail);
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setPrice(request.price());
        if (request.available() != null) {
            item.setAvailable(request.available());
        }
        return Mappers.toResponse(menuItemRepository.save(item));
    }

    private Restaurant findRestaurant(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant " + id + " not found"));
    }

    private void assertOwner(Restaurant restaurant, String email) {
        if (!restaurant.getOwner().getEmail().equals(email)) {
            throw new AccessDeniedException("You do not own this restaurant");
        }
    }

    private void apply(Restaurant restaurant, RestaurantRequest request) {
        restaurant.setName(request.name().trim());
        restaurant.setDescription(request.description());
        restaurant.setCity(request.city().trim());
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
