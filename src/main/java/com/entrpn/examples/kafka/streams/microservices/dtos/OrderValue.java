package com.entrpn.examples.kafka.streams.microservices.dtos;

public class OrderValue {

    private Order order;
    private Double value;

    public OrderValue() {
    }

    public OrderValue(Order order, Double value) {
        this.order = order;
        this.value = value;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
