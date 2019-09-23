package com.entrpn.examples.kafka.streams.microservices.dtos;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

public enum OrderState {
    CREATED("CREATED"), VALIDATED("VALIDATED"), FAILED("FAILED"), SHIPPED("SHIPPED"), UNKNOWN("UNKNOWN");

    private String value;

    private OrderState(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static OrderState create(String val) {
        for (OrderState state: OrderState.values()) {
            if (state.getValue().equalsIgnoreCase(val)) {
                return state;
            }
        }
        return UNKNOWN;
    }

}
