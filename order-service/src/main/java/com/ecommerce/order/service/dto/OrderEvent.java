package com.ecommerce.order.service.dto;

public class OrderEvent {

    private Long orderId;
    private String product;
    private Integer quantity;
    private Double price;

    public OrderEvent() {
    }

    public OrderEvent(Long orderId,
                      String product,
                      Integer quantity,
                      Double price) {
        this.orderId = orderId;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getProduct() {
        return product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}