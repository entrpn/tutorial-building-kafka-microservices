package com.entrpn.examples.kafka.streams.microservices.dtos;

public class OrderEnriched {

    private String id;
    private Long customerId;
    private String customerLevel;

    public OrderEnriched() {
    }

    public OrderEnriched(String id, Long customerId, String customerLevel) {
        this.id = id;
        this.customerId = customerId;
        this.customerLevel = customerLevel;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerLevel() {
        return customerLevel;
    }

    public void setCustomerLevel(String customerLevel) {
        this.customerLevel = customerLevel;
    }
}
