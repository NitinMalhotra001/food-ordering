package com.nitin.foodorder.repository;

import com.nitin.foodorder.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByRestaurantIdOrderByNameAsc(Long restaurantId);
}
