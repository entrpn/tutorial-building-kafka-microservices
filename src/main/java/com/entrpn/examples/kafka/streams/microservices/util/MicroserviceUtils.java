package com.entrpn.examples.kafka.streams.microservices.util;

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

    public static void setTimeout(final long timeout, final AsyncResponse response) {
        response.setTimeout(timeout, TimeUnit.MILLISECONDS);
        response.setTimeoutHandler(resp ->
                resp.resume(
                        Response.status(Response.Status.GATEWAY_TIMEOUT)
                                .entity("HTTP GET timed out after " + timeout + "ms\n")
                                .build()));
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
