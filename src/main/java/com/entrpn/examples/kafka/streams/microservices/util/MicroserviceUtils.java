package com.entrpn.examples.kafka.streams.microservices.util;

import com.entrpn.examples.kafka.streams.microservices.Schemas;
import com.entrpn.examples.kafka.streams.microservices.Service;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

public class MicroserviceUtils {

    private static final Logger log = LoggerFactory.getLogger(MicroserviceUtils.class);
    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String DEFAULT_SCHEMA_REGISTRY_URL = "http://localhost:8081";

    public static void setTimeout(final long timeout, final AsyncResponse response) {
        response.setTimeout(timeout, TimeUnit.MILLISECONDS);
        response.setTimeoutHandler(resp ->
                resp.resume(
                        Response.status(Response.Status.GATEWAY_TIMEOUT)
                                .entity("HTTP GET timed out after " + timeout + "ms\n")
                                .build()));
    }

    public static String parseArgsAndConfigure(final String[] args) {
        if (args.length > 2) {
            throw new IllegalArgumentException("usage: ... " +
                    "[<bootstrap.servers> (optional, default: " + DEFAULT_BOOTSTRAP_SERVERS + ")] " +
                    "[<schema.registry.url> (optional, default: " + DEFAULT_SCHEMA_REGISTRY_URL + ")] ");
        }
        final String bootstrapServers = args.length > 0 ? args[0] : "localhost:9092";
        final String schemaRegistryUrl = args.length > 1 ? args[1] : "http://localhost:8081";

        log.info("Connecting to Kafka cluster via bootstrap servers " + bootstrapServers);
        log.info("Connecting to Confluent schema registry at " + schemaRegistryUrl);
        log.error("TODO - uncomment schema registry line in MicroserviceUtils.parseArgsAndConfigure");
        // TODO
        //Schemas.configureSerdesWithSchemaRegistryUrl(schemaRegistryUrl);
        return bootstrapServers;
    }

    public static void addShutdownHookAndBlock(final Service service) throws InterruptedException {
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> service.stop());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                service.stop();
            } catch (final Exception ignored) {
            }
        }));
        Thread.currentThread().join();
    }

    public static Server startJetty(final int port, final Object binding) {
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

        context.setContextPath("/");

        final Server jettyServer = new Server(port);
        jettyServer.setHandler(context);

        final ResourceConfig rc = new ResourceConfig();
        rc.register(binding);
        rc.register(JacksonFeature.class);

        final ServletContainer sc = new ServletContainer(rc);
        final ServletHolder holder = new ServletHolder(sc);
        context.addServlet(holder, "/*");

        try {
            jettyServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("Listening on " + jettyServer.getURI());

        return jettyServer;

    }

}
