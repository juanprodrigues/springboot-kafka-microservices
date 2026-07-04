package com.ecommerce.billing.service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.billing.service.dto.OrderEvent;
import com.ecommerce.billing.service.entity.Invoice;
import com.ecommerce.billing.service.repository.InvoiceRepository;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final InvoiceRepository repository;

    public BillingService(InvoiceRepository repository) {
        this.repository = repository;
    }

    public void generateInvoice(OrderEvent event) {
        log.info("Generating invoice — orderId: {}, quantity: {}, price: {}",
                event.getOrderId(), event.getQuantity(), event.getPrice());

        Invoice invoice = new Invoice();
        invoice.setOrderId(event.getOrderId());

        double total = event.getPrice() * event.getQuantity();
        invoice.setTotal(total);
        invoice.setStatus("GENERATED");

        log.debug("Invoice details — orderId: {}, total: {}, status: GENERATED", event.getOrderId(), total);

        Invoice saved = repository.save(invoice);

        log.info("Invoice persisted — invoiceId: {}, orderId: {}, total: {}, status: {}",
                saved.getId(), saved.getOrderId(), saved.getTotal(), saved.getStatus());
    }
}