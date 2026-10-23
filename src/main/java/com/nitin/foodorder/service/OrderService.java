package com.nitin.foodorder.service;

import com.nitin.foodorder.dto.Mappers;
import com.nitin.foodorder.dto.OrderDtos.OrderResponse;
import com.nitin.foodorder.dto.OrderDtos.PlaceOrderRequest;
import com.nitin.foodorder.entity.CartItem;
import com.nitin.foodorder.entity.FoodOrder;
import com.nitin.foodorder.entity.MenuItem;
import com.nitin.foodorder.entity.OrderItem;
import com.nitin.foodorder.entity.OrderStatus;
import com.nitin.foodorder.entity.Restaurant;
import com.nitin.foodorder.entity.Role;
import com.nitin.foodorder.entity.User;
import com.nitin.foodorder.exception.BadRequestException;
import com.nitin.foodorder.exception.ResourceNotFoundException;
import com.nitin.foodorder.repository.CartItemRepository;
import com.nitin.foodorder.repository.FoodOrderRepository;
import com.nitin.foodorder.repository.RestaurantRepository;
import com.nitin.foodorder.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodOrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;

    public OrderService(UserRepository userRepository, CartItemRepository cartItemRepository,
                        FoodOrderRepository orderRepository, RestaurantRepository restaurantRepository) {
        this.userRepository = userRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
    }

    /**
     * Turns the customer's cart into an order in one transaction. Prices are read from the database
     * at this moment (never trusted from the client), unavailable items reject the whole order, and
     * the cart is cleared only if everything succeeds.
     */
    @Transactional
    public OrderResponse placeOrder(String email, PlaceOrderRequest request) {
        User user = findUser(email);
        List<CartItem> cart = cartItemRepository.findByUserId(user.getId());
        if (cart.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        Restaurant restaurant = cart.get(0).getMenuItem().getRestaurant();
        if (!restaurant.isActive()) {
            throw new BadRequestException(restaurant.getName() + " is not accepting orders");
        }

        FoodOrder order = new FoodOrder();
        order.setUser(user);
        order.setRestaurant(restaurant);
        order.setStatus(OrderStatus.PLACED);
        order.setDeliveryAddress(request.deliveryAddress().trim());

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem line : cart) {
            MenuItem menuItem = line.getMenuItem();
            if (!menuItem.isAvailable()) {
                throw new BadRequestException(menuItem.getName() + " is no longer available");
            }
            OrderItem item = new OrderItem();
            item.setMenuItemId(menuItem.getId());
            item.setName(menuItem.getName());
            item.setUnitPrice(menuItem.getPrice());
            item.setQuantity(line.getQuantity());
            order.addItem(item);
            total = total.add(menuItem.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        }
        order.setTotalAmount(total);

        FoodOrder saved = orderRepository.save(order);
        cartItemRepository.deleteAll(cart);
        return Mappers.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> myOrders(String email, Pageable pageable) {
        User user = findUser(email);
        return orderRepository.findByUserId(user.getId(), pageable).map(Mappers::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> restaurantOrders(Long restaurantId, String email, Pageable pageable) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant " + restaurantId + " not found"));
        if (!restaurant.getOwner().getEmail().equals(email)) {
            throw new AccessDeniedException("You do not own this restaurant");
        }
        return orderRepository.findByRestaurantId(restaurantId, pageable).map(Mappers::toResponse);
    }

    /** Visible to the customer who placed it, the owner of the restaurant, and admins. */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, String email) {
        FoodOrder order = findOrder(orderId);
        User actor = findUser(email);
        if (!isCustomer(order, actor) && !isOwner(order, actor) && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You cannot view this order");
        }
        return Mappers.toResponse(order);
    }

    /**
     * Restaurant owners (and admins) drive the order through its lifecycle. The customer may only
     * cancel an order that is still PLACED. Every change must be a legal transition.
     */
    @Transactional
    public OrderResponse updateStatus(Long orderId, String email, OrderStatus newStatus) {
        FoodOrder order = findOrder(orderId);
        User actor = findUser(email);

        boolean owner = isOwner(order, actor);
        boolean admin = actor.getRole() == Role.ADMIN;
        boolean customer = isCustomer(order, actor);

        if (!owner && !admin) {
            if (customer && newStatus == OrderStatus.CANCELLED) {
                if (order.getStatus() != OrderStatus.PLACED) {
                    throw new BadRequestException("You can only cancel an order that has just been placed");
                }
            } else {
                throw new AccessDeniedException("You cannot change the status of this order");
            }
        }

        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new BadRequestException(
                    "Cannot move an order from " + order.getStatus() + " to " + newStatus);
        }
        order.setStatus(newStatus);
        return Mappers.toResponse(orderRepository.save(order));
    }

    private boolean isCustomer(FoodOrder order, User actor) {
        return order.getUser().getId().equals(actor.getId());
    }

    private boolean isOwner(FoodOrder order, User actor) {
        return order.getRestaurant().getOwner().getId().equals(actor.getId());
    }

    private FoodOrder findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + id + " not found"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
