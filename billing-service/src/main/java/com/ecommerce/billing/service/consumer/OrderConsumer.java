package com.ecommerce.billing.service.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecommerce.billing.service.dto.OrderEvent;
import com.ecommerce.billing.service.service.BillingService;

@Component
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    private final BillingService billingService;

    public OrderConsumer(BillingService billingService) {
        this.billingService = billingService;
    }

    @KafkaListener(
            topics = "${kafka.topic.order-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(OrderEvent event) {
        log.info("Received OrderEvent from Kafka — orderId: {}, product: {}, quantity: {}, price: {}",
                event.getOrderId(), event.getProduct(), event.getQuantity(), event.getPrice());

        try {
            billingService.generateInvoice(event);
            log.info("OrderEvent processed successfully — orderId: {}", event.getOrderId());
        } catch (Exception ex) {
            log.error("Failed to process OrderEvent — orderId: {} — error: {}",
                    event.getOrderId(), ex.getMessage(), ex);
        }
    }
}