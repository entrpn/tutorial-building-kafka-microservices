package com.entrpn.examples.kafka.streams.microservices;

public interface Service {

    void start(String bootstrapServers, String stateDir);

    void stop();

}
