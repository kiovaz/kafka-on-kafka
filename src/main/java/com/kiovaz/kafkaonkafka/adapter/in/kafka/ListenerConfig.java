package com.kiovaz.kafkaonkafka.adapter.in.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
class ListenerConfig {

    /**
     * When a listener throws (only when a result could not be published), retry the same message every 2 seconds
     * without limit instead of skipping it. Messages that cannot be deserialized are not retried: they are logged
     * and skipped by this handler's default rules.
     */
    @Bean
    DefaultErrorHandler errorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(2000L, FixedBackOff.UNLIMITED_ATTEMPTS));
    }
}
