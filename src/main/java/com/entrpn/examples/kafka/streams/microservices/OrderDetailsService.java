package com.entrpn.examples.kafka.streams.microservices;

import com.entrpn.examples.kafka.streams.microservices.dtos.*;
import com.entrpn.examples.kafka.streams.microservices.util.MicroserviceUtils;
import com.entrpn.examples.kafka.streams.microservices.util.MonitoringInterceptorUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.entrpn.examples.kafka.streams.microservices.dtos.OrderValidationResult.FAIL;
import static com.entrpn.examples.kafka.streams.microservices.dtos.OrderValidationResult.PASS;

public class OrderDetailsService implements Service {

    private static final Logger log = LoggerFactory.getLogger(OrderDetailsService.class);

    private final String CONSUMER_GROUP_ID = getClass().getSimpleName();
    private KafkaConsumer<String, Order> consumer;
    private KafkaProducer<String, OrderValidation> producer;

    private volatile boolean running;

    // Disable Exactly Once Semantics to enable Confluent Monitoring Interceptors
    private boolean eosEnabled = false;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void start(String bootstrapServers, String stateDir) {
        running = true;
        executorService.execute(() -> startService(bootstrapServers));
        log.info("started service " + getClass().getSimpleName());
    }

    @Override
    public void stop() {
        running = false;
        try {
            executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            log.info("Failed to stop " + getClass().getSimpleName() + " in 10000 ms");
        }
        log.info(getClass().getSimpleName() + " was stopped");
    }

    private void close() {
        if (producer != null) {
            producer.close();
        }
        if (consumer != null) {
            consumer.close();
        }
    }

    private void startService(final String bootstrapServers) {

        startConsumer(bootstrapServers);
        startProducer(bootstrapServers);

        try {

            final Map<TopicPartition, OffsetAndMetadata> consumedOffsets = new HashMap();
            consumer.subscribe(Collections.singletonList(Schemas.Topics.ORDERS.name()));
            if (eosEnabled) {
                producer.initTransactions();
            }

            while (running) {
                final ConsumerRecords<String, Order> records = consumer.poll(Duration.ofMillis(100));
                if (records.count() > 0) {
                    if (eosEnabled) {
                        producer.beginTransaction();
                    }

                    for (final ConsumerRecord<String, Order> record : records) {
                        final Order order = record.value();
                        log.info("topic: " + record.topic());
                        log.info("orderKey: " + record.key());
                        log.info("order: " + order);
                        log.info("order value size: " + record.serializedValueSize());
                        if (order != null && OrderState.CREATED.equals(order.getState())) {
                            producer.send(result(order, isValid(order) ? PASS : FAIL));
                            if (eosEnabled) {
                                recordOffset(consumedOffsets, record);
                            }
                        }
                    }
                    if (eosEnabled) {
                        producer.sendOffsetsToTransaction(consumedOffsets, CONSUMER_GROUP_ID);
                        producer.commitTransaction();
                    }

                }
            }
        } finally {
            close();
        }
    }

    private void recordOffset(final Map<TopicPartition, OffsetAndMetadata> consumedOffsets,
                              final ConsumerRecord<String, Order> record) {
        final OffsetAndMetadata nextOffset = new OffsetAndMetadata(record.offset() + 1);
        consumedOffsets.put(new TopicPartition(record.topic(), record.partition()), nextOffset);
    }

    private ProducerRecord<String, OrderValidation> result(final Order order,
                                                           final OrderValidationResult passOrFail) {
        return new ProducerRecord(
                Schemas.Topics.ORDER_VALIDATIONS.name(),
                order.getId(),
                new OrderValidation(order.getId(), OrderValidationType.ORDER_DETAILS_CHECK, passOrFail)
        );
    }

    private void startConsumer(String bootstrapServers) {
        final Properties consumerConfig = new Properties();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, !eosEnabled);
        consumerConfig.put(ConsumerConfig.CLIENT_ID_CONFIG, "order-dtails-service-consumer");

        consumer = new KafkaConsumer(consumerConfig,
                Schemas.Topics.ORDERS.getKeySerde().deserializer(),
                Schemas.Topics.ORDERS.getValueSerde().deserializer());
    }

    private void startProducer(final String bootstrapService) {
        final Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapService);
        if (eosEnabled) {
            producerConfig.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "OrderDetailsServiceInstance1");
        }
        producerConfig.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        producerConfig.put(ProducerConfig.RETRIES_CONFIG, String.valueOf(Integer.MAX_VALUE));
        producerConfig.put(ProducerConfig.ACKS_CONFIG, "all");
        producerConfig.put(ProducerConfig.CLIENT_ID_CONFIG, "order-details-service-producer");
        MonitoringInterceptorUtils.maybeConfigureInterceptorsProducer(producerConfig);

        producer = new KafkaProducer<>(producerConfig,
                Schemas.Topics.ORDER_VALIDATIONS.getKeySerde().serializer(),
                Schemas.Topics.ORDER_VALIDATIONS.getValueSerde().serializer());

    }

    private boolean isValid(final Order order) {
        log.info("customerId: " + order.getCustomerId());
        log.info("quantity: " + order.getQuantity());
        log.info("price: " + order.getPrice());
        log.info("product: " + order.getProduct());
        if (order.getCustomerId() == null) {
            return false;
        }
        if (order.getQuantity() < 0) {
            return false;
        }
        if (order.getPrice() < 0) {
            return false;
        }
        return order.getProduct() != null;
    }

    public static void main(final String[] args) throws Exception {
        final OrderDetailsService service = new OrderDetailsService();
        service.start(MicroserviceUtils.parseArgsAndConfigure(args), "/tmp/kafka-streams");
        MicroserviceUtils.addShutdownHookAndBlock(service);
    }
}
