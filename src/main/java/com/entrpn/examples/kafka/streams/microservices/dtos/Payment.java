package com.entrpn.examples.kafka.streams.microservices.dtos;

public class Payment {

    private String id;
    private String orderId;
    private String ccy;
    private Double amount;

    public Payment(){}

    public Payment(String id, String orderId, String ccy, Double amount) {
        this.id = id;
        this.orderId = orderId;
        this.ccy = ccy;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCcy() {
        return ccy;
    }

    public void setCcy(String ccy) {
        this.ccy = ccy;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
