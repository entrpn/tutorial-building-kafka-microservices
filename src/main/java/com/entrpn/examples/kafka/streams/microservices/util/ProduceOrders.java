package com.entrpn.examples.kafka.streams.microservices.util;

import com.entrpn.examples.kafka.streams.microservices.dtos.Order;
import com.entrpn.examples.kafka.streams.microservices.serdes.JsonSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

import static com.entrpn.examples.kafka.streams.microservices.dtos.OrderState.CREATED;

public class ProduceOrders {

    public static void main(final String[] args) {

        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        MonitoringInterceptorUtils.maybeConfigureInterceptorsProducer(props);

        KafkaProducer<String, Order> producer = new KafkaProducer(props, new StringSerializer(), new JsonSerializer());

        try {

            int orderId = 50;

            while (true) {
                final Order order = new Order(String.valueOf(orderId), 15L, CREATED.getValue(), "UNDERPANTS", 3, 5.00d);
                final ProducerRecord<String,Order> record = new ProducerRecord<>("orders",order.getId(),order);
                producer.send(record);
                Thread.sleep(10000L);
                orderId++;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
