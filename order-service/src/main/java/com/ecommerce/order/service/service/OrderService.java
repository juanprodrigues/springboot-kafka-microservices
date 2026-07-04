package com.ecommerce.order.service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.ecommerce.order.service.dto.OrderEvent;
import com.ecommerce.order.service.entity.Order;
import com.ecommerce.order.service.repository.OrderRepository;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Value("${kafka.topic.order-created}")
    private String orderCreatedTopic;

    private final OrderRepository repository;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderService(OrderRepository repository,
                        KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Order create(Order order) {
        log.info("Creating order — product: {}, quantity: {}, price: {}",
                order.getProduct(), order.getQuantity(), order.getPrice());

        Order saved = repository.save(order);
        log.debug("Order persisted to database — orderId: {}", saved.getId());

        OrderEvent event = new OrderEvent(
                saved.getId(),
                saved.getProduct(),
                saved.getQuantity(),
                saved.getPrice()
        );

        log.info("Publishing OrderEvent to topic '{}' — orderId: {}", orderCreatedTopic, event.getOrderId());

        kafkaTemplate.send(orderCreatedTopic, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish OrderEvent to topic '{}' — orderId: {} — error: {}",
                        orderCreatedTopic, event.getOrderId(), ex.getMessage(), ex);
            } else {
                log.info("OrderEvent published successfully — topic: {}, partition: {}, offset: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

        log.info("Order creation completed — orderId: {}", saved.getId());
        return saved;
    }
}