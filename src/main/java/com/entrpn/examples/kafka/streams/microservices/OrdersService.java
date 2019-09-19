package com.entrpn.examples.kafka.streams.microservices;

import com.entrpn.examples.kafka.streams.microservices.dtos.Order;
import com.entrpn.examples.kafka.streams.microservices.models.HostStoreInfo;
import com.entrpn.examples.kafka.streams.microservices.util.MicroserviceUtils;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.entrpn.examples.kafka.streams.microservices.Schemas.Topics.ORDERS;
import static com.entrpn.examples.kafka.streams.microservices.util.ProducerUtils.startProducer;
import static com.entrpn.examples.kafka.streams.microservices.util.StreamsUtils.baseStreamsConfig;

@Path("v1")
public class OrdersService implements Service {

    private static final Logger log = LoggerFactory.getLogger(OrdersService.class);

    private final String SERVICE_APP_ID = getClass().getSimpleName();
    private static final String CALL_TIMEOUT = "10000";
    private static final String ORDERS_STORE_NAME = "orders-store";

    private final String host;
    private int port;
    private Server jettyServer;

    private KafkaProducer<String, Order> producer;

    private MetadataService metadataService;
    private KafkaStreams streams = null;

    //In a real implementation we would need to (a) support outstanding requests for the same Id/filter from
    // different users and (b) periodically purge old entries from this map.
    private final Map<String, FilteredResponse<String, Order>> outstandingRequests = new ConcurrentHashMap<>();

    public OrdersService(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    @GET
    @Path("/orders/{id}")
    public void getOrder(@PathParam("id") final String id,
                         @QueryParam("timeout") @DefaultValue(CALL_TIMEOUT) final Long timeout,
                         @Suspended final AsyncResponse response) {
        MicroserviceUtils.setTimeout(timeout, response);

        fetchLocal(id, response, (k, v) -> true);

    }

    @POST
    @ManagedAsync
    @Path("/orders")
    public void createOrder(final Order order,
                            @QueryParam("timeout") @DefaultValue(CALL_TIMEOUT) final Long timeout,
                            @Suspended final AsyncResponse response) {
        log.debug("createOrder");
        MicroserviceUtils.setTimeout(timeout, response);

        try {
            producer.send(new ProducerRecord(ORDERS.name(), order.getId(), order),
                    callback(response, order.getId()));
        } catch (final Exception e) {
            log.error("error: " + e);
        }
    }

    @Override
    public void start(final String bootstrapServers, final String stateDir) {
        jettyServer = MicroserviceUtils.startJetty(port, this);
        port = jettyServer.getURI().getPort(); //update port, in case port was zero

        producer = startProducer(bootstrapServers, ORDERS);
        streams = startKStreams(bootstrapServers);

        log.info("Started Service " + getClass().getSimpleName());
    }

    @Override
    public void stop() {

        if (streams != null) {
            streams.close();
        }

        if (producer != null) {
            producer.close();
        }

        if (jettyServer != null) {
            try {
                jettyServer.stop();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        log.info("Stopped Service");
    }

    /**
     * Fetch the order from the local materialized view
     *
     * @param id            ID to fetch
     * @param asyncResponse the response to call once completed
     * @param predicate     a filter that for this fetch, so for example we might fetch only VALIDATED
     *                      orders.
     */
    private void fetchLocal(final String id, final AsyncResponse asyncResponse, final Predicate<String, Order> predicate) {
        log.info("running GET on this node");
        final Order order = ordersStore().get(id);
        if (order == null || !predicate.test(id, order)) {
            asyncResponse.resume("Not found");
        } else {
            asyncResponse.resume(order);
        }
    }

    private ReadOnlyKeyValueStore<String, Order> ordersStore() {
        ReadOnlyKeyValueStore store = streams.store(ORDERS_STORE_NAME, QueryableStoreTypes.keyValueStore());
        store.
        log.debug("Store number of entries: " + store.approximateNumEntries());

        return store;

    }

    private Properties config(final String bootstrapServers) {
        final Properties props = baseStreamsConfig(bootstrapServers, "/tmp/kafka-streams", SERVICE_APP_ID);
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, host + ":" + port);
        return props;
    }

    private KafkaStreams startKStreams(final String bootstrapServers) {
        final KafkaStreams streams = new KafkaStreams(
                createOrdersMaterializedView().build(),
                config(bootstrapServers));

        metadataService = new MetadataService(streams);
        //streams.cleanUp(); // don't do this in prod as it clears your state stores

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

        return streams;

    }

    /**
     * Create a table of orders which we can query. When the table is updated
     * we check to see if there is an outstanding HTTP GET request waiting to be
     * fulfilled.
     */
    private StreamsBuilder createOrdersMaterializedView() {
        final StreamsBuilder builder = new StreamsBuilder();
        builder.table(ORDERS.name(), Consumed.with(ORDERS.keySerde(), ORDERS.valueSerde()), Materialized.as(ORDERS_STORE_NAME))
                .toStream().foreach(this::maybeCompleteLongPollGet);
        return builder;
    }

    private void maybeCompleteLongPollGet(final String id, final Order order) {
        log.debug("maybeCompleteLongPollGet");
        final FilteredResponse<String, Order> callback = outstandingRequests.get(id);
        if (callback != null && callback.predicate.test(id, order)) {
            callback.asyncResponse.resume(order);
        }
    }

    private Callback callback(final AsyncResponse response, final String orderId) {

        return (metadata, exception) -> {
            if (exception != null) {
                response.resume(exception);
            } else {
                try {
                    final Response uri = Response.created(new URI("/v1/orders/" + orderId)).build();
                    response.resume(uri);
                } catch (final URISyntaxException e) {
                    e.printStackTrace();
                    response.resume(e);
                }
            }
        };
    }

    public static void main(final String[] args) throws Exception {
        final String bootstrapServers = args.length > 0 ? args[0] : "localhost:9092";
        // will use this later
        final String schemaRegistryUrl = args.length > 1 ? args[1] : "http://localhost:8081";
        final String restHostname = args.length > 2 ? args[2] : "localhost";
        final String restPort = args.length > 3 ? args[3] : null;

        final OrdersService service = new OrdersService(restHostname, restPort == null ? 0 : Integer.valueOf(restPort));
        service.start(bootstrapServers, "/tmp/kafka-streams");

    }

    class FilteredResponse<K, V> {
        private final AsyncResponse asyncResponse;
        private final Predicate<K, V> predicate;

        FilteredResponse(final AsyncResponse asyncResponse, final Predicate<K, V> predicate) {
            this.asyncResponse = asyncResponse;
            this.predicate = predicate;
        }
    }
}
