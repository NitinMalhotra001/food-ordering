package com.nitin.foodorder.repository;

import com.nitin.foodorder.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    /** Pass an empty string (not null) to skip a filter. */
    @Query("select r from Restaurant r where r.active = true "
            + "and (:city = '' or lower(r.city) = lower(:city)) "
            + "and (:q = '' or lower(r.name) like lower(concat('%', :q, '%')))")
    Page<Restaurant> search(@Param("city") String city, @Param("q") String q, Pageable pageable);
}
