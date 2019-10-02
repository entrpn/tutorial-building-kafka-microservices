package com.entrpn.examples.kafka.streams.microservices;

import com.entrpn.examples.kafka.streams.microservices.serdes.ProductTypeSerde;
import io.confluent.examples.streams.avro.microservices.*;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
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

        public Serde<K> getKeySerde() {
            return keySerde;
        }

        public Serde<V> getValueSerde() {
            return valueSerde;
        }

        public String toString() {
            return name;
        }
    }

    public static class Topics {
        public static final Map<String, Topic<?, ?>> ALL = new HashMap();
        public static Topic<String, Order> ORDERS;

        public static Topic<String, OrderValidation> ORDER_VALIDATIONS;

        public static Topic<String, Payment> PAYMENTS;

        public static Topic<Long, Customer> CUSTOMERS;

        public static Topic<String, OrderEnriched> ORDERS_ENRICHED;

        public static Topic<Product, Integer> WAREHOUSE_INVENTORY;

        static {
            createTopics();
        }

        private static void createTopics() {
            ORDERS = new Topic("orders", Serdes.String(), new SpecificAvroSerde());
            ORDER_VALIDATIONS = new Topic("order-validations", Serdes.String(), new SpecificAvroSerde());
            PAYMENTS = new Topic("payments", Serdes.String(), new SpecificAvroSerde());
            CUSTOMERS = new Topic("customers", Serdes.Long(), new SpecificAvroSerde());
            ORDERS_ENRICHED = new Topic("orders-enriched", Serdes.String(), new SpecificAvroSerde());
            WAREHOUSE_INVENTORY = new Topic("warehouse-inventory", new ProductTypeSerde(), Serdes.Integer());
        }

    }
}
