package com.nitin.foodorder.repository;

import com.nitin.foodorder.entity.FoodOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    Page<FoodOrder> findByUserId(Long userId, Pageable pageable);

    Page<FoodOrder> findByRestaurantId(Long restaurantId, Pageable pageable);
}
