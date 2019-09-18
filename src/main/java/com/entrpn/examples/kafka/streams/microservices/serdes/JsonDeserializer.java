package com.entrpn.examples.kafka.streams.microservices.serdes;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class JsonDeserializer<T> implements Deserializer {

    private Class<T> type;

    public JsonDeserializer(Class type) {
        this.type = type;
    }

    @Override
    public void configure(Map configs, boolean isKey) {

    }

    @Override
    public Object deserialize(String topic, byte[] data) {
        return null;
    }

    @Override
    public Object deserialize(String topic, Headers headers, byte[] data) {
        return null;
    }

    @Override
    public void close() {

    }
}
