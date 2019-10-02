package com.entrpn.examples.kafka.streams.microservices.util;

import com.entrpn.examples.kafka.streams.microservices.Schemas;

import io.confluent.examples.streams.avro.microservices.Order;
import io.confluent.examples.streams.avro.microservices.OrderValidation;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Serdes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerTest {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerTest.class);
    private static final String CONSUMER_GROUP_ID = ConsumerTest.class.getSimpleName();

    public static void main(String[] args) {

        KafkaConsumer consumer = startConsumer();
        consumer.subscribe(Arrays.asList(Schemas.Topics.ORDERS.name(), Schemas.Topics.ORDER_VALIDATIONS.name()));

        int i = 0;
        while (i < 2000) {
            final ConsumerRecords<String, ?> records = consumer.poll(Duration.ofMillis(1000));

            if (records.count() > 0) {
                for (final ConsumerRecord<String, ?> record : records) {
                    logger.info("key: " + record.key());

                    if (record.value() instanceof Order) {
                        final Order order = (Order) record.value();
                        logger.info("This is an Order record");
                        logger.info("customerId: " + order.getCustomerId());
                    } else if (record.value() instanceof OrderValidation) {
                        final OrderValidation orderValidation = (OrderValidation) record.value();
                        logger.info("this is an OrderValidation record");
                        logger.info("Check Type: " + orderValidation.getCheckType());
                    } else {
                        logger.error("Record value type not known");
                    }

                }
            }

        }

    }

    private static KafkaConsumer startConsumer() {
        final Properties consumerConfig = new Properties();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerConfig.put(ConsumerConfig.CLIENT_ID_CONFIG, "order-dtails-service-consumer");

        return new KafkaConsumer(consumerConfig,
                Serdes.String().deserializer(),
                new SpecificAvroSerde().deserializer());
    }
}
