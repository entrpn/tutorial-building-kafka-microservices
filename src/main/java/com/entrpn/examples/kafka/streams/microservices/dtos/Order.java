package com.entrpn.examples.kafka.streams.microservices.dtos;

public class Order {

    private String id;
    private Long customerId;
    private OrderState state;
    private String product;
    private int quantity;
    private double price;

    public Order() {
    }

    public Order(final String id, final Long customerId, final String state, final String product, final int quantity, final double price) {
        this.id = id;
        this.customerId = customerId;
        this.state = OrderState.valueOf(state);
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(final Long customerId) {
        this.customerId = customerId;
    }

    public OrderState getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = OrderState.valueOf(state);
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(final String product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(final int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(final double price) {
        this.price = price;
    }

}
