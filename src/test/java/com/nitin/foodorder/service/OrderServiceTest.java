package com.nitin.foodorder.service;

import com.nitin.foodorder.dto.OrderDtos.OrderResponse;
import com.nitin.foodorder.dto.OrderDtos.PlaceOrderRequest;
import com.nitin.foodorder.entity.CartItem;
import com.nitin.foodorder.entity.FoodOrder;
import com.nitin.foodorder.entity.MenuItem;
import com.nitin.foodorder.entity.OrderStatus;
import com.nitin.foodorder.entity.Restaurant;
import com.nitin.foodorder.entity.Role;
import com.nitin.foodorder.entity.User;
import com.nitin.foodorder.exception.BadRequestException;
import com.nitin.foodorder.repository.CartItemRepository;
import com.nitin.foodorder.repository.FoodOrderRepository;
import com.nitin.foodorder.repository.RestaurantRepository;
import com.nitin.foodorder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private FoodOrderRepository orderRepository;
    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private OrderService orderService;

    private User customer;
    private User owner;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        customer = user(1L, "customer@test.com", Role.CUSTOMER);
        owner = user(2L, "owner@test.com", Role.RESTAURANT_OWNER);
        restaurant = new Restaurant();
        restaurant.setId(10L);
        restaurant.setName("Spice Hub");
        restaurant.setCity("Delhi");
        restaurant.setOwner(owner);
    }

    @Test
    void placeOrderFailsWhenCartIsEmpty() {
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of());

        assertThrows(BadRequestException.class,
                () -> orderService.placeOrder("customer@test.com", new PlaceOrderRequest("12 MG Road")));
        verify(orderRepository, never()).save(any(FoodOrder.class));
    }

    @Test
    void placeOrderFailsWhenAnItemIsUnavailable() {
        MenuItem soldOut = menuItem(100L, "Biryani", "180.00", false);
        List<CartItem> cart = List.of(cartLine(soldOut, 1));
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserId(1L)).thenReturn(cart);

        assertThrows(BadRequestException.class,
                () -> orderService.placeOrder("customer@test.com", new PlaceOrderRequest("12 MG Road")));
        verify(orderRepository, never()).save(any(FoodOrder.class));
    }

    @Test
    void placeOrderComputesTotalFromDatabasePricesAndClearsCart() {
        List<CartItem> cart = List.of(
                cartLine(menuItem(100L, "Paneer Tikka", "100.00", true), 2),
                cartLine(menuItem(101L, "Naan", "50.50", true), 1));
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserId(1L)).thenReturn(cart);
        when(orderRepository.save(any(FoodOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.placeOrder("customer@test.com", new PlaceOrderRequest("12 MG Road"));

        assertEquals(OrderStatus.PLACED, response.status());
        assertEquals(0, new BigDecimal("250.50").compareTo(response.totalAmount()));
        assertEquals(2, response.items().size());
        verify(cartItemRepository).deleteAll(cart);
    }

    @Test
    void customerCannotCancelOnceOrderIsConfirmed() {
        FoodOrder order = order(OrderStatus.CONFIRMED);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));

        assertThrows(BadRequestException.class,
                () -> orderService.updateStatus(50L, "customer@test.com", OrderStatus.CANCELLED));
    }

    @Test
    void customerCanCancelAPlacedOrder() {
        FoodOrder order = order(OrderStatus.PLACED);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(FoodOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateStatus(50L, "customer@test.com", OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, response.status());
    }

    @Test
    void customerCannotConfirmTheirOwnOrder() {
        FoodOrder order = order(OrderStatus.PLACED);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));

        assertThrows(AccessDeniedException.class,
                () -> orderService.updateStatus(50L, "customer@test.com", OrderStatus.CONFIRMED));
    }

    @Test
    void ownerCannotSkipStatusSteps() {
        FoodOrder order = order(OrderStatus.PLACED);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));

        assertThrows(BadRequestException.class,
                () -> orderService.updateStatus(50L, "owner@test.com", OrderStatus.DELIVERED));
    }

    @Test
    void ownerCanConfirmAPlacedOrder() {
        FoodOrder order = order(OrderStatus.PLACED);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(orderRepository.save(any(FoodOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateStatus(50L, "owner@test.com", OrderStatus.CONFIRMED);

        assertEquals(OrderStatus.CONFIRMED, response.status());
    }

    private FoodOrder order(OrderStatus status) {
        FoodOrder order = new FoodOrder();
        order.setId(50L);
        order.setUser(customer);
        order.setRestaurant(restaurant);
        order.setStatus(status);
        order.setDeliveryAddress("12 MG Road");
        order.setTotalAmount(new BigDecimal("100.00"));
        return order;
    }

    private User user(Long id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setName(email);
        user.setEmail(email);
        user.setPassword("hashed");
        user.setRole(role);
        return user;
    }

    private MenuItem menuItem(Long id, String name, String price, boolean available) {
        MenuItem item = new MenuItem();
        item.setId(id);
        item.setRestaurant(restaurant);
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        item.setAvailable(available);
        return item;
    }

    private CartItem cartLine(MenuItem menuItem, int quantity) {
        CartItem line = new CartItem();
        line.setUser(customer);
        line.setMenuItem(menuItem);
        line.setQuantity(quantity);
        return line;
    }
}
