package com.entrpn.examples.kafka.streams.microservices.serdes;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class JsonSerializer implements Serializer {

    private static final Logger log = LoggerFactory.getLogger(JsonSerializer.class);

    @Override
    public void configure(Map configs, boolean isKey) {

    }

    @Override
    public byte[] serialize(String topic, Object data) {
        log.debug("topic: " + topic);
        return serialize(data);
    }

    @Override
    public byte[] serialize(String topic, Headers headers, Object data) {
        log.debug("topic: " + topic);
        log.debug("headers: " + headers);
        return serialize(data);
    }

    private byte[] serialize(Object data) {
        byte[] retval = null;
        ObjectMapper om = new ObjectMapper();
        try {
            retval = om.writeValueAsString(data).getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.debug("payload size: " + retval.length);

        return retval;
    }

    @Override
    public void close() {

    }
}
