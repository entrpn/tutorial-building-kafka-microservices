package com.entrpn.examples.kafka.streams.microservices.processors;

import com.entrpn.examples.kafka.streams.microservices.EmailService;
import com.entrpn.examples.kafka.streams.microservices.dtos.Order;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class OrdersProcessor implements Processor<String, Order> {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private ProcessorContext context;

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
    }

    @Override
    public void process(String key, Order value) {

        context.schedule(Duration.ofMillis(30000), PunctuationType.STREAM_TIME, (timestamp -> {
            log.info("timestamp: " + timestamp);
            log.info("key: " + key);
            log.info("orderId: " + value.getId());
        }));
    }

    @Override
    public void close() {

    }
}
