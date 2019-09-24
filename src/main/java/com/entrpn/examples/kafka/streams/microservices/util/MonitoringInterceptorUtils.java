package com.entrpn.examples.kafka.streams.microservices.util;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class MonitoringInterceptorUtils {

    private static final String CONSUMER_INTERCEPTOR = "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor";
    private static final String PRODUCER_INTERCEPTOR = "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor";
    private static final Logger Logger = LoggerFactory.getLogger(MonitoringInterceptorUtils.class);

    public static void maybeConfigureInterceptorsConsumer(final Properties consumerConfig) {
        if(hasMonitoringConsumerInterceptor()) {
            consumerConfig.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, CONSUMER_INTERCEPTOR);
        }
    }

    public static void maybeConfigureInterceptorsProducer(final Properties producerConfig) {
        if (hasMonitoringProducerInterceptor()) {
            producerConfig.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, PRODUCER_INTERCEPTOR);
        }
    }

    private static boolean hasMonitoringProducerInterceptor() {
        return hasMonitoringInterceptor(PRODUCER_INTERCEPTOR);
    }

    private static boolean hasMonitoringInterceptor(final String className) {
        boolean hasInterceptor = true;
        try {
            Class.forName(className);
        } catch (final ClassNotFoundException e) {
            final String interceptorTypeShortName = className.substring(className.lastIndexOf('.'));
            Logger.info("{} not found, skipping", interceptorTypeShortName);
            hasInterceptor = false;
        }
        return hasInterceptor;
    }

    public static void maybeConfigureInterceptorsStreams(final Properties streamsConfig) {
        if (hasMonitoringConsumerInterceptor() && hasMonitoringProducerInterceptor()) {
            streamsConfig.put(StreamsConfig.producerPrefix(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG), PRODUCER_INTERCEPTOR);
            streamsConfig.put(StreamsConfig.mainConsumerPrefix(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG), CONSUMER_INTERCEPTOR);
        }
    }

    private static boolean hasMonitoringConsumerInterceptor() {
        return hasMonitoringInterceptor(CONSUMER_INTERCEPTOR);
    }

}
