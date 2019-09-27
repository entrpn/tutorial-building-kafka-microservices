package com.entrpn.examples.kafka.streams.microservices.util;

import com.entrpn.examples.kafka.streams.microservices.dtos.Customer;
import com.entrpn.examples.kafka.streams.microservices.serdes.JsonSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;

import java.util.Properties;

public class ProduceCustomers {

    public static void main(final String[] args) {

        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        MonitoringInterceptorUtils.maybeConfigureInterceptorsProducer(props);

        KafkaProducer<Long, Customer> producer = new KafkaProducer(props, new LongSerializer(), new JsonSerializer());

        try {

            while (true) {
                final Customer customer = new Customer(15L, "Franz", "Kafka", "frans@thedarkside.net", "oppression street, prague, cze", "gold");
                final ProducerRecord<Long, Customer> record = new ProducerRecord("customers", customer.getId(), customer);
                producer.send(record);
                Thread.sleep(10000L);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
