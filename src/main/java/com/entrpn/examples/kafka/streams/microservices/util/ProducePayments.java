package com.entrpn.examples.kafka.streams.microservices.util;

import com.entrpn.examples.kafka.streams.microservices.dtos.Payment;
import com.entrpn.examples.kafka.streams.microservices.serdes.JsonSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class ProducePayments {

    public static void main(final String[] args) {

        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        MonitoringInterceptorUtils.maybeConfigureInterceptorsProducer(props);

        KafkaProducer<String, Payment> producer = new KafkaProducer<String, Payment>(props, new StringSerializer(), new JsonSerializer());

        try {

            int orderId = 50;

            while (true) {
                final Payment payment = new Payment("Payment:1234", String.valueOf(orderId), "CZK", 1000.00d);
                final ProducerRecord<String, Payment> record = new ProducerRecord("payments", payment.getId(), payment);
                producer.send(record);
                Thread.sleep(10000L);
                orderId++;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

}
