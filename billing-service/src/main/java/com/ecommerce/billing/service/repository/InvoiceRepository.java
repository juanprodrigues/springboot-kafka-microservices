package com.ecommerce.billing.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.billing.service.entity.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}