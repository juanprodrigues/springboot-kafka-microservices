package com.ecommerce.billing.service.service;

import org.springframework.stereotype.Service;

import com.ecommerce.billing.service.dto.OrderEvent;
import com.ecommerce.billing.service.entity.Invoice;
import com.ecommerce.billing.service.repository.InvoiceRepository;

@Service
public class BillingService {

    private final InvoiceRepository repository;

    public BillingService(InvoiceRepository repository) {
        this.repository = repository;
    }

    public void generateInvoice(OrderEvent event) {

        Invoice invoice = new Invoice();

        invoice.setOrderId(event.getOrderId());
        invoice.setTotal(event.getPrice() * event.getQuantity());
        invoice.setStatus("GENERATED");

        repository.save(invoice);
    }
}