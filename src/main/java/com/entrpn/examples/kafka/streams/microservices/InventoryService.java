package com.entrpn.examples.kafka.streams.microservices;

import com.entrpn.examples.kafka.streams.microservices.dtos.Order;
import com.entrpn.examples.kafka.streams.microservices.dtos.OrderState;
import com.entrpn.examples.kafka.streams.microservices.dtos.OrderValidation;
import com.entrpn.examples.kafka.streams.microservices.dtos.OrderValidationType;
import com.entrpn.examples.kafka.streams.microservices.util.MicroserviceUtils;
import com.entrpn.examples.kafka.streams.microservices.util.StreamsUtils;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.To;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.entrpn.examples.kafka.streams.microservices.dtos.OrderValidationResult.FAIL;
import static com.entrpn.examples.kafka.streams.microservices.dtos.OrderValidationResult.PASS;

public class InventoryService implements Service {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    public static final String SERVICE_APP_ID = "InventoryService";
    public static final String RESERVED_STOCK_STORE_NAME = "store-of-reserved-stock";
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
        } catch (InterruptedException e) {
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
                .stream(Schemas.Topics.ORDERS.name(), Consumed.with(Schemas.Topics.ORDERS.getKeySerde(), Schemas.Topics.ORDERS.getValueSerde()));

        final KTable<String, Integer> warehouseInventory = builder
                .table(Schemas.Topics.WAREHOUSE_INVENTORY.name(), Consumed.with(Schemas.Topics.WAREHOUSE_INVENTORY.getKeySerde(), Schemas.Topics.WAREHOUSE_INVENTORY.getValueSerde()));

        //Create a store to reserve inventory whilst the order is processed.
        // This will be prepopulated from kafka before the service starts processing
        final StoreBuilder reservedStock = Stores
                .keyValueStoreBuilder(Stores.persistentKeyValueStore(RESERVED_STOCK_STORE_NAME),
                        Schemas.Topics.WAREHOUSE_INVENTORY.getKeySerde(), Serdes.Long())
                .withLoggingEnabled(new HashMap<>());

        builder.addStateStore(reservedStock);

        orders.selectKey((id, order) -> order.getProduct())
                .filter((id, order) -> OrderState.CREATED.equals(order.getState()))
                .join(warehouseInventory, KeyValue::new, Joined.with(Schemas.Topics.WAREHOUSE_INVENTORY.getKeySerde(),
                        Schemas.Topics.ORDERS.getValueSerde(), Serdes.Integer()))
                .transform(InventoryValidator::new, RESERVED_STOCK_STORE_NAME)
                .to(Schemas.Topics.ORDER_VALIDATIONS.name(), Produced.with(Schemas.Topics.ORDER_VALIDATIONS.getKeySerde(),
                        Schemas.Topics.ORDER_VALIDATIONS.getValueSerde()));

        return new KafkaStreams(builder.build(),
                StreamsUtils.baseStreamsConfig(bootstrapServers, stateDir, SERVICE_APP_ID));

    }

    private static class InventoryValidator implements Transformer<String, KeyValue<Order, Integer>, KeyValue<String, OrderValidation>> {

        private KeyValueStore<String, Long> reservedStockStore;

        @Override
        public void init(ProcessorContext context) {
            reservedStockStore = (KeyValueStore<String, Long>) context.getStateStore(RESERVED_STOCK_STORE_NAME);
        }

        @Override
        public KeyValue<String, OrderValidation> transform(String key, KeyValue<Order, Integer> value) {
            //Process each order/inventory pair one at a time
            final OrderValidation validated;
            final Order order = value.key;
            final Integer warehouseStockCount = value.value;

            //Look up locally 'reserved' stock from our state store
            Long reserved = reservedStockStore.get(order.getProduct());
            if (reserved == null) {
                reserved = 0L;
            }

            //If there is enough stock available(considering both warehouse inventory and reserved stock) validate the order.
            if (warehouseStockCount - reserved - order.getQuantity() >= 0) {
                reservedStockStore.put(order.getProduct(), reserved + order.getQuantity());
                validated = new OrderValidation(order.getId(), OrderValidationType.INVENTORY_CHECK, PASS);
            } else {
                validated = new OrderValidation(order.getId(), OrderValidationType.INVENTORY_CHECK, FAIL);
            }
            return KeyValue.pair(validated.getOrderId(), validated);

        }

        @Override
        public void close() {

        }
    }

    public static void main(final String[] args) throws Exception {
        final InventoryService service = new InventoryService();
        service.start(MicroserviceUtils.parseArgsAndConfigure(args), "/tmp/kafka-streams");
        MicroserviceUtils.addShutdownHookAndBlock(service);
    }
}
