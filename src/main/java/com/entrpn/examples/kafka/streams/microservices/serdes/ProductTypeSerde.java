package com.entrpn.examples.kafka.streams.microservices.serdes;

import io.confluent.examples.streams.avro.microservices.Product;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

//Streams doesn't provide an Enum serdes so just create one here.
public class ProductTypeSerde implements Serde<Product> {

    @Override
    public void configure(final Map<String, ?> map, final boolean b) {
    }

    @Override
    public void close() {
    }

    @Override
    public Serializer<Product> serializer() {
        return new Serializer<Product>() {
            @Override
            public void configure(final Map<String, ?> map, final boolean b) {
            }

            @Override
            public byte[] serialize(final String topic, final Product pt) {
                return pt.toString().getBytes();
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public Deserializer<Product> deserializer() {
        return new Deserializer<Product>() {
            @Override
            public void configure(final Map<String, ?> map, final boolean b) {
            }

            @Override
            public Product deserialize(final String topic, final byte[] bytes) {
                return Product.valueOf(new String(bytes));
            }

            @Override
            public void close() {
            }
        };
    }
}
