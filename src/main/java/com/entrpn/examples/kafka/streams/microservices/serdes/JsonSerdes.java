package com.entrpn.examples.kafka.streams.microservices.serdes;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

public class JsonSerdes<T> extends Serdes.WrapperSerde<T> {

    public JsonSerdes(Class type) {
        super(new JsonSerializer(), new JsonDeserializer<T>(type));
    }
}
