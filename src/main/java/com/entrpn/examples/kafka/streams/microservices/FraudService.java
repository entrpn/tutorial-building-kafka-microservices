package com.entrpn.examples.kafka.streams.microservices;

import com.entrpn.examples.kafka.streams.microservices.util.MicroserviceUtils;
import com.entrpn.examples.kafka.streams.microservices.util.StreamsUtils;
import io.confluent.examples.streams.avro.microservices.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.confluent.examples.streams.avro.microservices.OrderValidationResult.FAIL;
import static io.confluent.examples.streams.avro.microservices.OrderValidationResult.PASS;

public class FraudService implements Service {

    private static final Logger log = LoggerFactory.getLogger(FraudService.class);
    private final String SERVICE_APP_ID = getClass().getSimpleName();

    private static final int FRAUD_LIMIT = 2000;

    private KafkaStreams streams;


    @Override
    public void start(String bootstrapServers, String stateDir) {
        streams = processStreams(bootstrapServers, stateDir);
        streams.cleanUp();

        final CountDownLatch startLatch = new CountDownLatch(1);

        streams.setStateListener(((newState, oldState) -> {
            if (newState == KafkaStreams.State.RUNNING && oldState != KafkaStreams.State.RUNNING) {
                startLatch.countDown();
            }
        }));

        streams.start();

        try {
            if (!startLatch.await(60, TimeUnit.SECONDS)) {
                throw new RuntimeException("Streams never finished rebalancing on startup");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Started Service " + getClass().getSimpleName());

    }

    @Override
    public void stop() {
        if (streams != null) {
            streams.close();
        }
    }

    private KafkaStreams processStreams(final String bootstrapServers, final String stateDir) {

        final StreamsBuilder builder = new StreamsBuilder();
        final KStream<String, Order> orders = builder
                .stream(Schemas.Topics.ORDERS.name(), Consumed.with(Schemas.Topics.ORDERS.getKeySerde(), Schemas.Topics.ORDERS.getValueSerde()))
                .filter((id, order) -> OrderState.CREATED.equals(order.getState()));

        //Create an aggregate of the total value by customer and hold it with the order. We use session windows
        // to detect periods of activity.
        final KTable<Windowed<Long>, OrderValue> aggregate = orders
                .groupBy((id, order) -> order.getCustomerId(), Grouped.with(Serdes.Long(), Schemas.Topics.ORDERS.getValueSerde()))
                .windowedBy(SessionWindows.with(Duration.ofHours(1)))
                .aggregate(OrderValue::new,
                        (custId, order, total) -> {
                            log.info("customerId: " + custId);
                            log.info("order: " + order.getId());
                            log.info("total: " + total.getValue() + order.getQuantity() * order.getPrice());
                            return new OrderValue(order, (total.getValue()) + order.getQuantity() * order.getPrice());
                        },
                        (k, a, b) -> simpleMerge(a, b),
                        Materialized.with(null, Schemas.ORDER_VALUE_SERDE));

        // Ditch the windowing and rekey
        final KStream<String, OrderValue> ordersWithTotals = aggregate
                .toStream((windowedKey, orderValue) -> windowedKey.key())
                .filter((key, value) -> value != null) // When elements are evicted from a session window they create delete events. Filter these out
                .selectKey((id, orderValue) -> orderValue.getOrder().getId());

        // Now branch stream into two, for pass and fail, based on whether the windowed total is over Fraud Limit.
        final KStream<String, OrderValue>[] forks = ordersWithTotals.branch(
                (id, orderValue) -> orderValue.getValue() >= FRAUD_LIMIT,
                (id, orderValue) -> orderValue.getValue() < FRAUD_LIMIT);

        forks[0].mapValues(
                orderValue -> new OrderValidation(orderValue.getOrder().getId(), OrderValidationType.FRAUD_CHECK, FAIL))
                .to(Schemas.Topics.ORDER_VALIDATIONS.name(), Produced.with(Schemas.Topics.ORDER_VALIDATIONS.getKeySerde(), Schemas.Topics.ORDER_VALIDATIONS.getValueSerde()));

        forks[1].mapValues(
                orderValue -> new OrderValidation(orderValue.getOrder().getId(), OrderValidationType.FRAUD_CHECK, PASS))
                .to(Schemas.Topics.ORDER_VALIDATIONS.name(), Produced.with(Schemas.Topics.ORDER_VALIDATIONS.getKeySerde(), Schemas.Topics.ORDER_VALIDATIONS.getValueSerde()));

        //disable caching to ensure a complete aggregate changelog. This is a little trick we need to apply
        //as caching in Kafka Streams will conflate subsequent updates for the same key. Disabling caching ensures
        //we get a complete "changelog" from the aggregate(...) step above (i.e. every input event will have a
        //corresponding output event.
        final Properties props = StreamsUtils.baseStreamsConfig(bootstrapServers, stateDir, SERVICE_APP_ID);
        props.setProperty(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");

        final Topology topology = builder.build();

        log.info("Topology: " + topology);

        return new KafkaStreams(topology, props);

    }

    private OrderValue simpleMerge(final OrderValue a, final OrderValue b) {
        return new OrderValue(b.getOrder(), ((a == null || a.getValue() == 0) ? b.getValue() : a.getValue() + b.getValue()));
    }

    public static void main(String[] args) throws Exception {
        final FraudService service = new FraudService();
        service.start(MicroserviceUtils.parseArgsAndConfigure(args), "/tmp/kafka-streams");
        MicroserviceUtils.addShutdownHookAndBlock(service);
    }

}
