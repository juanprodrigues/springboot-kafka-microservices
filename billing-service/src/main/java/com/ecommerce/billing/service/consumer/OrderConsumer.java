package com.ecommerce.billing.service.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecommerce.billing.service.dto.OrderEvent;
import com.ecommerce.billing.service.service.BillingService;

@Component
public class OrderConsumer {

    private final BillingService billingService;

    public OrderConsumer(BillingService billingService) {
        this.billingService = billingService;
    }

    @KafkaListener(
            topics = "order-created",
            groupId = "billing-group"
    )
    public void consume(OrderEvent event) {

        System.out.println(
                "Order received: " + event.getOrderId()
        );

        billingService.generateInvoice(event);
    }
}