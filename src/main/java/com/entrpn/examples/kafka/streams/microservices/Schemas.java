package com.entrpn.examples.kafka.streams.microservices;

import com.entrpn.examples.kafka.streams.microservices.dtos.Order;
import com.entrpn.examples.kafka.streams.microservices.serdes.JsonSerdes;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

import java.util.HashMap;
import java.util.Map;

public class Schemas {

    public static class Topic<K, V> {
        private final String name;
        private final Serde<K> keySerde;
        private final Serde<V> valueSerde;

        Topic(final String name, final Serde<K> keySerde, final Serde<V> valueSerde) {
            this.name = name;
            this.keySerde = keySerde;
            this.valueSerde = valueSerde;
            Topics.ALL.put(name, this);
        }

        public String name() {
            return name;
        }

        public Serde<K> keySerde() {
            return keySerde;
        }

        public Serde<V> valueSerde() {
            return valueSerde;
        }

        public String toString() {
            return name;
        }
    }

    public static class Topics {
        public static final Map<String, Topic<?, ?>> ALL = new HashMap();
        public static Topic<String, Order> ORDERS;

        static {
            createTopics();
        }

        private static void createTopics() {
            ORDERS = new Topic("orders", Serdes.String(), new JsonSerdes<Order>(Order.class));
        }

    }
}
