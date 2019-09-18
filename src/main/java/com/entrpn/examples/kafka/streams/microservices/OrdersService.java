package com.entrpn.examples.kafka.streams.microservices;

import com.entrpn.examples.kafka.streams.microservices.dtos.Order;
import com.entrpn.examples.kafka.streams.microservices.util.MicroserviceUtils;
import org.apache.kafka.clients.producer.*;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static com.entrpn.examples.kafka.streams.microservices.Schemas.Topics.ORDERS;

@Path("v1")
public class OrdersService implements Service {

    private static final Logger log = LoggerFactory.getLogger(OrdersService.class);

    private final String SERVICE_APP_ID = getClass().getSimpleName();
    private static final String CALL_TIMEOUT = "10000";

    private final String host;
    private int port;
    private Server jettyServer;

    private KafkaProducer<String, Order> producer;

    public OrdersService(final String host, final int port) {
        this.host = host;
        this.port = port;
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

        log.info("Started Service " + getClass().getSimpleName());
    }

    @Override
    public void stop() {
        if (jettyServer != null) {
            try {
                jettyServer.stop();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        log.info("Stopped Service");
    }

    private static <T> KafkaProducer startProducer(final String bootstrapServers,
                                                   final Schemas.Topic<String, T> topic) {
        final Properties producerConfig = new Properties();

        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfig.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        producerConfig.put(ProducerConfig.RETRIES_CONFIG, String.valueOf(Integer.MAX_VALUE));
        producerConfig.put(ProducerConfig.ACKS_CONFIG, "all");
        producerConfig.put(ProducerConfig.CLIENT_ID_CONFIG, "order-sender");

        return new KafkaProducer(producerConfig,
                topic.getKeySerde().serializer(),
                topic.getValueSerde().serializer());
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
}
