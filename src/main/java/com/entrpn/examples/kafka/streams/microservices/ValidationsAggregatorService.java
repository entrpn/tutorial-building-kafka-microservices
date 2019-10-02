package com.entrpn.examples.kafka.streams.microservices;

import com.entrpn.examples.kafka.streams.microservices.util.StreamsUtils;
import io.confluent.examples.streams.avro.microservices.Order;
import io.confluent.examples.streams.avro.microservices.OrderState;
import io.confluent.examples.streams.avro.microservices.OrderValidation;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.entrpn.examples.kafka.streams.microservices.util.MicroserviceUtils.addShutdownHookAndBlock;
import static com.entrpn.examples.kafka.streams.microservices.util.MicroserviceUtils.parseArgsAndConfigure;
import static io.confluent.examples.streams.avro.microservices.OrderValidationResult.FAIL;
import static io.confluent.examples.streams.avro.microservices.OrderValidationResult.PASS;

public class ValidationsAggregatorService implements Service {

    private Logger log = LoggerFactory.getLogger(ValidationsAggregatorService.class);

    private final int WINDOW_TIME_IN_MINS = 2;

    private KafkaStreams streams;
    private final String SERVICE_APP_ID = getClass().getSimpleName();


    @Override
    public void start(String bootstrapServers, String stateDir) {
        final CountDownLatch startLatch = new CountDownLatch(1);

        streams = aggregateOrderValidations(bootstrapServers, stateDir);
        streams.cleanUp();

        streams.setStateListener(((newState, oldState) -> {
            if (newState == KafkaStreams.State.RUNNING && oldState != KafkaStreams.State.RUNNING) {
                startLatch.countDown();
            }
        }));

        streams.start();

        try {
            if (!startLatch.await(60, TimeUnit.SECONDS)) {
                throw new RuntimeException("Stream never finished rebalancing on startup");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    @Override
    public void stop() {
        if (streams != null) {
            streams.close();
        }
    }

    private KafkaStreams aggregateOrderValidations(String bootstrapServers, String stateDir) {
        final int numberOfRules = 3;

        final StreamsBuilder builder = new StreamsBuilder();

        final KStream<String, OrderValidation> validations = builder
                .stream(Schemas.Topics.ORDER_VALIDATIONS.name(), Consumed.with(Schemas.Topics.ORDER_VALIDATIONS.getKeySerde(), Schemas.Topics.ORDER_VALIDATIONS.getValueSerde()));

        final KStream<String, Order> orders = builder
                .stream(Schemas.Topics.ORDERS.name(), Consumed.with(Schemas.Topics.ORDERS.getKeySerde(), Schemas.Topics.ORDERS.getValueSerde()))
                .filter((id, order) -> OrderState.CREATED.equals(order.getState()));

        // if rules pass then validate the order
        validations
                .groupByKey(Grouped.with(Schemas.Topics.ORDER_VALIDATIONS.getKeySerde(), Schemas.Topics.ORDER_VALIDATIONS.getValueSerde()))
                .windowedBy(SessionWindows.with(Duration.ofMinutes(WINDOW_TIME_IN_MINS)))
                .aggregate(
                        () -> 0L,
                        (id, result, total) -> PASS.equals(result.getValidationResult()) ? total + 1 : total,
                        (k, a, b) -> b == null ? a : b,
                        Materialized.with(null, Serdes.Long())
                )
                .toStream((windowedKey, total) -> windowedKey.key())
                .filter((k1, v) -> v != null)
                .filter((k, total) -> total >= numberOfRules)
                .join(
                        orders,
                        (id, order) -> {
                            order.setState(OrderState.VALIDATED);
                            return order;
                        },
                        JoinWindows.of(Duration.ofMinutes(WINDOW_TIME_IN_MINS)),
                        Joined.with(Schemas.Topics.ORDERS.getKeySerde(), Serdes.Long(), Schemas.Topics.ORDERS.getValueSerde())
                )
                .groupByKey(Grouped.with(Schemas.Topics.ORDERS.getKeySerde(), Schemas.Topics.ORDERS.getValueSerde()))
                .reduce((order, v1) -> order)
                .toStream()
                .peek((key, value) -> {
                    log.info("key: " + key);
                })
                .to(Schemas.Topics.ORDERS.name(), Produced.with(Schemas.Topics.ORDERS.getKeySerde(), Schemas.Topics.ORDERS.getValueSerde()));

        // If any rule fails then fail the other
        validations
                .filter((id, orderValidation) -> FAIL.equals(orderValidation.getValidationResult()))
                .join(orders, (id, order) -> {
                            order.setState(OrderState.FAILED);
                            return order;
                        },
                        JoinWindows.of(Duration.ofMinutes(WINDOW_TIME_IN_MINS)),
                        Joined.with(Schemas.Topics.ORDERS.getKeySerde(), Schemas.Topics.ORDER_VALIDATIONS.getValueSerde(), Schemas.Topics.ORDERS.getValueSerde())
                )
                .groupByKey(Grouped.with(Schemas.Topics.ORDERS.getKeySerde(), Schemas.Topics.ORDERS.getValueSerde()))
                .reduce((order, v1) -> order)
                .toStream().to(Schemas.Topics.ORDERS.name(), Produced.with(Schemas.Topics.ORDERS.getKeySerde(), Schemas.Topics.ORDERS.getValueSerde()));

        final Topology topology = builder.build();

        return new KafkaStreams(topology,
                StreamsUtils.baseStreamsConfig(bootstrapServers, stateDir, SERVICE_APP_ID));


    }

    public static void main(final String[] args) throws Exception {
        final String bootstrapServers = parseArgsAndConfigure(args);
        final ValidationsAggregatorService service = new ValidationsAggregatorService();
        service.start(bootstrapServers, "/tmp/kafka-streams");
        addShutdownHookAndBlock(service);
    }

}
