package com.ecommerce.order.service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.order.service.entity.Order;
import com.ecommerce.order.service.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public Order create(@RequestBody Order order) {
        log.info("POST /api/orders — product: {}, quantity: {}, price: {}",
                order.getProduct(), order.getQuantity(), order.getPrice());

        long start = System.currentTimeMillis();

        Order saved = service.create(order);

        long elapsed = System.currentTimeMillis() - start;
        log.info("POST /api/orders — completed in {}ms — orderId: {}", elapsed, saved.getId());

        return saved;
    }
}