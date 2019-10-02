package com.entrpn.examples.kafka.streams.microservices.dtos;

import io.confluent.examples.streams.avro.microservices.Order;
import io.confluent.examples.streams.avro.microservices.OrderState;
import io.confluent.examples.streams.avro.microservices.Product;

public class OrderBean {

    private String id;
    private Long customerId;
    private OrderState state;
    private Product product;
    private int quantity;
    private double price;

    public OrderBean() {
    }

    public OrderBean(final String id, final Long customerId, final OrderState state, final Product product, final int quantity, final double price) {
        this.id = id;
        this.customerId = customerId;
        this.state = state;
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

    public void setState(final OrderState state) {
        this.state = state;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(final Product product) {
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

    @Override
    public String toString() {
        return "OrderBean{" +
                "id='" + id + '\'' +
                ", customerId=" + customerId +
                ", state=" + state +
                ", product=" + product +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }

    public static OrderBean toBean(final Order order) {
        return new OrderBean(order.getId(),
                order.getCustomerId(),
                order.getState(),
                order.getProduct(),
                order.getQuantity(),
                order.getPrice());
    }

    public static Order fromBean(final OrderBean order) {
        return new Order(order.getId(),
                order.getCustomerId(),
                order.getState(),
                order.getProduct(),
                order.getQuantity(),
                order.getPrice());
    }

}
