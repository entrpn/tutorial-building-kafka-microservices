package com.entrpn.examples.kafka.streams.microservices.serdes;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class JsonDeserializer<T> implements Deserializer {

    private static final Logger log = LoggerFactory.getLogger(JsonSerializer.class);

    private Class<T> type;

    public JsonDeserializer(Class type) {
        this.type = type;
    }

    @Override
    public void configure(Map configs, boolean isKey) {

    }

    @Override
    public Object deserialize(String topic, byte[] data) {

        ObjectMapper mapper = new ObjectMapper();
        T obj = null;
        try {
            obj = mapper.readValue(data, type);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return obj;
    }

    @Override
    public Object deserialize(String topic, Headers headers, byte[] data) {

        ObjectMapper mapper = new ObjectMapper();
        T obj = null;
        try {
            obj = mapper.readValue(data, type);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return obj;
    }

    @Override
    public void close() {

    }
}
