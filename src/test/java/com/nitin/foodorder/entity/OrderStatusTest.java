package com.nitin.foodorder.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void happyPathIsAllowedStepByStep() {
        assertTrue(OrderStatus.PLACED.canTransitionTo(OrderStatus.CONFIRMED));
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PREPARING));
        assertTrue(OrderStatus.PREPARING.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY));
        assertTrue(OrderStatus.OUT_FOR_DELIVERY.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    void cannotSkipSteps() {
        assertFalse(OrderStatus.PLACED.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY));
    }

    @Test
    void cannotGoBackwards() {
        assertFalse(OrderStatus.PREPARING.canTransitionTo(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.PLACED));
    }

    @Test
    void cancellationOnlyBeforePreparation() {
        assertTrue(OrderStatus.PLACED.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.PREPARING.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.OUT_FOR_DELIVERY.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void finalStatesHaveNoNextStep() {
        assertEquals(0, OrderStatus.DELIVERED.allowedNext().size());
        assertEquals(0, OrderStatus.CANCELLED.allowedNext().size());
    }
}
