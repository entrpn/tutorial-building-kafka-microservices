package com.entrpn.examples.kafka.streams.microservices.util;

import io.confluent.examples.streams.avro.microservices.Order;
import io.confluent.examples.streams.avro.microservices.Product;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

import static io.confluent.examples.streams.avro.microservices.OrderState.CREATED;

public class ProduceOrders {

    public static void main(final String[] args) {

        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        MonitoringInterceptorUtils.maybeConfigureInterceptorsProducer(props);

        KafkaProducer<String, Order> producer = new KafkaProducer(props, new StringSerializer(), new SpecificAvroSerializer());

        try {

            int orderId = 50;

            while (true) {
                final Order order = new Order(String.valueOf(orderId), 15L, CREATED, Product.UNDERPANTS, 3, 300.00d);
                final ProducerRecord<String, Order> record = new ProducerRecord<>("orders", order.getId(), order);
                producer.send(record);
                Thread.sleep(10000L);
                orderId++;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
