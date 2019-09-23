package com.entrpn.examples.kafka.streams.microservices.dtos;

public class OrderValidation {

    private String orderId;
    private OrderValidationType checkType;
    private OrderValidationResult validationResult;

    public OrderValidation() {
    }

    public OrderValidation(final String orderId, final OrderValidationType checkType, final OrderValidationResult validationResult) {
        this.orderId = orderId;
        this.checkType = checkType;
        this.validationResult = validationResult;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public OrderValidationType getCheckType() {
        return checkType;
    }

    public void setCheckType(OrderValidationType checkType) {
        this.checkType = checkType;
    }

    public OrderValidationResult getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(OrderValidationResult validationResult) {
        this.validationResult = validationResult;
    }
}
