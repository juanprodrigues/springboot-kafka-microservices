package com.ecommerce.order.service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.ecommerce.order.service.dto.OrderEvent;
import com.ecommerce.order.service.entity.Order;
import com.ecommerce.order.service.repository.OrderRepository;

@Service
public class OrderService {

    private static final String TOPIC = "order-created";

    private final OrderRepository repository;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderService(OrderRepository repository,
                        KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Order create(Order order) {

        Order saved = repository.save(order);

        OrderEvent event = new OrderEvent(
                saved.getId(),
                saved.getProduct(),
                saved.getQuantity(),
                saved.getPrice()
        );

        kafkaTemplate.send(TOPIC, event).whenComplete((result, ex) -> {
            if (ex != null) {
                System.out.println("Error sending message: " + ex.getMessage());
            } else {
                System.out.println("Message sent to topic: " +
                        result.getRecordMetadata().topic());
            }
        });
        

        return saved;
    }
}