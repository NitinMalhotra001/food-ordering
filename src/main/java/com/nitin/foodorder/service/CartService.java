package com.nitin.foodorder.service;

import com.nitin.foodorder.dto.CartDtos.AddCartItemRequest;
import com.nitin.foodorder.dto.CartDtos.CartItemResponse;
import com.nitin.foodorder.dto.CartDtos.CartResponse;
import com.nitin.foodorder.dto.Mappers;
import com.nitin.foodorder.entity.CartItem;
import com.nitin.foodorder.entity.MenuItem;
import com.nitin.foodorder.entity.User;
import com.nitin.foodorder.exception.BadRequestException;
import com.nitin.foodorder.exception.ResourceNotFoundException;
import com.nitin.foodorder.repository.CartItemRepository;
import com.nitin.foodorder.repository.MenuItemRepository;
import com.nitin.foodorder.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private static final int MAX_QUANTITY_PER_ITEM = 50;

    private final CartItemRepository cartItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;

    public CartService(CartItemRepository cartItemRepository, MenuItemRepository menuItemRepository,
                       UserRepository userRepository) {
        this.cartItemRepository = cartItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        return buildCart(findUser(email).getId());
    }

    @Transactional
    public CartResponse addItem(String email, AddCartItemRequest request) {
        User user = findUser(email);
        MenuItem menuItem = menuItemRepository.findById(request.menuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item " + request.menuItemId() + " not found"));
        if (!menuItem.isAvailable()) {
            throw new BadRequestException(menuItem.getName() + " is currently unavailable");
        }

        List<CartItem> current = cartItemRepository.findByUserId(user.getId());
        if (!current.isEmpty()) {
            Long cartRestaurantId = current.get(0).getMenuItem().getRestaurant().getId();
            if (!cartRestaurantId.equals(menuItem.getRestaurant().getId())) {
                throw new BadRequestException(
                        "Your cart has items from another restaurant. Clear the cart before adding this item");
            }
        }

        CartItem line = cartItemRepository.findByUserIdAndMenuItemId(user.getId(), menuItem.getId())
                .orElseGet(() -> {
                    CartItem created = new CartItem();
                    created.setUser(user);
                    created.setMenuItem(menuItem);
                    created.setQuantity(0);
                    return created;
                });
        int newQuantity = line.getQuantity() + request.quantity();
        if (newQuantity > MAX_QUANTITY_PER_ITEM) {
            throw new BadRequestException("You can order at most " + MAX_QUANTITY_PER_ITEM + " of one item");
        }
        line.setQuantity(newQuantity);
        cartItemRepository.save(line);
        return buildCart(user.getId());
    }

    @Transactional
    public CartResponse updateQuantity(String email, Long menuItemId, int quantity) {
        User user = findUser(email);
        CartItem line = cartItemRepository.findByUserIdAndMenuItemId(user.getId(), menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item is not in your cart"));
        line.setQuantity(quantity);
        cartItemRepository.save(line);
        return buildCart(user.getId());
    }

    @Transactional
    public CartResponse removeItem(String email, Long menuItemId) {
        User user = findUser(email);
        CartItem line = cartItemRepository.findByUserIdAndMenuItemId(user.getId(), menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item is not in your cart"));
        cartItemRepository.delete(line);
        return buildCart(user.getId());
    }

    @Transactional
    public void clear(String email) {
        User user = findUser(email);
        cartItemRepository.deleteAll(cartItemRepository.findByUserId(user.getId()));
    }

    private CartResponse buildCart(Long userId) {
        List<CartItem> lines = cartItemRepository.findByUserId(userId);
        List<CartItemResponse> items = lines.stream().map(Mappers::toResponse).toList();
        BigDecimal total = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long restaurantId = lines.isEmpty() ? null : lines.get(0).getMenuItem().getRestaurant().getId();
        return new CartResponse(restaurantId, items, total);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
