package com.ecommerce.billing.service.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.billing.service.entity.Invoice;
import com.ecommerce.billing.service.repository.InvoiceRepository;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceRepository repository;

    public InvoiceController(InvoiceRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Invoice> getAll() {
        log.info("GET /api/invoices — fetching all invoices");

        long start = System.currentTimeMillis();
        List<Invoice> invoices = repository.findAll();
        long elapsed = System.currentTimeMillis() - start;

        log.info("GET /api/invoices — returned {} record(s) in {}ms", invoices.size(), elapsed);
        return invoices;
    }
}