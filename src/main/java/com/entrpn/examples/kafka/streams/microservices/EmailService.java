package com.entrpn.examples.kafka.streams.microservices;

import com.entrpn.examples.kafka.streams.microservices.dtos.Customer;
import com.entrpn.examples.kafka.streams.microservices.dtos.Order;
import com.entrpn.examples.kafka.streams.microservices.dtos.OrderEnriched;
import com.entrpn.examples.kafka.streams.microservices.dtos.Payment;
import com.entrpn.examples.kafka.streams.microservices.util.StreamsUtils;
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

public class EmailService implements Service {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final String SERVICE_APP_ID = getClass().getSimpleName();

    private KafkaStreams streams;
    private final Emailer emailer;

    public EmailService(final Emailer emailer) {
        this.emailer = emailer;
    }

    @Override
    public void start(String bootstrapServers, String stateDir) {
        streams = processStreams(bootstrapServers, stateDir);
        streams.cleanUp();
        final CountDownLatch startLatch = new CountDownLatch(1);
        streams.setStateListener((newState, oldState) -> {
            if (newState == KafkaStreams.State.RUNNING && oldState != KafkaStreams.State.RUNNING) {
                startLatch.countDown();
            }
        });

        streams.start();

        try {
            if (!startLatch.await(60, TimeUnit.SECONDS)) {
                throw new RuntimeException("Streams never finished rebalancing on startup");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        if (streams != null) {
            streams.close();
        }
    }

    private KafkaStreams processStreams(final String bootstrapServers, final String stateDir) {

        final StreamsBuilder builder = new StreamsBuilder();

        //Create the streams/table for the join
        final KStream<String, Order> orders = builder.stream(Schemas.Topics.ORDERS.name(),
                Consumed.with(Schemas.Topics.ORDERS.getKeySerde(), Schemas.Topics.ORDERS.getValueSerde()));

        final KStream<String, Payment> payments = builder.stream(Schemas.Topics.PAYMENTS.name(),
                Consumed.with(Schemas.Topics.PAYMENTS.getKeySerde(), Schemas.Topics.PAYMENTS.getValueSerde()))
                .selectKey((s, payment) -> payment.getOrderId());

        final GlobalKTable<Long, Customer> customers = builder.globalTable(Schemas.Topics.CUSTOMERS.name(),
                Consumed.with(Schemas.Topics.CUSTOMERS.getKeySerde(), Schemas.Topics.CUSTOMERS.getValueSerde()));

        final Joined<String, Order, Payment> serdes = Joined.with(Schemas.Topics.ORDERS.getKeySerde(),
                Schemas.Topics.ORDERS.getValueSerde(), Schemas.Topics.PAYMENTS.getValueSerde());

        orders.join(payments, EmailTuple::new,
                JoinWindows.of(Duration.ofMinutes(1)), serdes)
                .join(customers, (key, value) -> value.order.getCustomerId(), EmailTuple::setCustomer)
                .peek((key, value) -> {
                    log.info("************************");
                    log.info("key (orderId): " + key);
                    log.info("payment.orderId: " + value.payment.getOrderId());
                    log.info("order.orderId: " + value.order.getId());
                    log.info("customerId: " + value.order.getCustomerId());
                    log.info("orderState: " + value.order.getState());
                    log.info("product: " + value.order.getProduct());
                    log.info("quantity: " + value.order.getQuantity());
                    log.info("price: " + value.order.getPrice());
                    log.info("getAmount: " + value.payment.getAmount());
                    log.info("ccy: " + value.payment.getCcy());
                    emailer.sendEmail(value);
                });

        orders.join(
                customers,
                (orderId, order) -> order.getCustomerId(),
                (order, customer) -> new OrderEnriched(order.getId(), order.getCustomerId(), customer.getLevel())
        ).to(
                (orderId, orderEnriched, record) -> {
                    log.info("orderEnriched customerLevel: " + orderEnriched.getCustomerLevel());
                    return orderEnriched.getCustomerLevel();
                },
                Produced.with(Schemas.Topics.ORDERS_ENRICHED.getKeySerde(),
                        Schemas.Topics.ORDERS_ENRICHED.getValueSerde()));

        Topology topology = builder.build();

        log.info("topology: " + topology.describe());

        return new KafkaStreams(topology, StreamsUtils.baseStreamsConfig(bootstrapServers, stateDir, SERVICE_APP_ID));
    }


    public static void main(final String[] args) throws Exception {
        final EmailService service = new EmailService(new LoggingEmailer());
        service.start(parseArgsAndConfigure(args), "/tmp/kafka-streams");
        addShutdownHookAndBlock(service);
    }

    public static class LoggingEmailer implements Emailer {

        @Override
        public void sendEmail(EmailTuple details) {
            log.warn("Sending email: \nCustomer:{}\nOrder:{}\nPayment{}", details.customer, details.order, details.payment);
        }
    }

    interface Emailer {
        void sendEmail(EmailTuple details);
    }

    public class EmailTuple {
        public Order order;
        public Payment payment;
        public Customer customer;

        public EmailTuple(final Order order, final Payment payment) {
            this.order = order;
            this.payment = payment;
        }

        EmailTuple setCustomer(final Customer customer) {
            this.customer = customer;
            return this;
        }

        @Override
        public String toString() {
            return "EmailTuple{" +
                    "order=" + order +
                    ", payment=" + payment +
                    ", customer=" + customer +
                    '}';
        }


    }

}
