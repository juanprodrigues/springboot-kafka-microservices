package com.ecommerce.order.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.order.service.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}