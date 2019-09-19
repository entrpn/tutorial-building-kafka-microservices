package com.entrpn.examples.kafka.streams.microservices.util;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.state.RocksDBConfigSetter;
import org.rocksdb.Options;

import java.util.Map;
import java.util.Properties;

public class StreamsUtils {

    public static Properties baseStreamsConfig(final String bootstrapServers,
                                               final String stateDir,
                                               final String appId) {
        return baseStreamsConfig(bootstrapServers, stateDir, appId, false);
    }


    public static Properties baseStreamsConfig(final String bootstrapServers,
                                               final String stateDir,
                                               final String appId,
                                               final boolean enableEOS) {
        final Properties config = new Properties();
        // Workaround for a known issue with RocksDB in environments where you have only 1 cpu core.
        config.put(StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG, CustomRocksDBConfig.class);
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, appId);
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        final String processingGuaranteeConfig = enableEOS ? "exactly_once" : "at_least_once";
        config.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, processingGuaranteeConfig);
        config.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1); //commit as fast as possible
        config.put(StreamsConfig.consumerPrefix(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG), 30000);
        MonitoringInterceptorUtils.maybeConfigureInterceptorsStreams(config);

        return config;
    }

    public static class CustomRocksDBConfig implements RocksDBConfigSetter {

        @Override
        public void setConfig(final String storeName, final Options options,
                              final Map<String, Object> configs) {
            // Workaround: We must ensure that the parallelism is set to >= 2.  There seems to be a known
            // issue with RocksDB where explicitly setting the parallelism to 1 causes issues (even though
            // 1 seems to be RocksDB's default for this configuration).
            final int compactionParallelism = Math.max(Runtime.getRuntime().availableProcessors(), 2);
            // Set number of compaction threads (but not flush threads).
            options.setIncreaseParallelism(compactionParallelism);
        }
    }

}
