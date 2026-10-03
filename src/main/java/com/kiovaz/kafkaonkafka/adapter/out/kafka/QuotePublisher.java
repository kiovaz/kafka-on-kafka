package com.kiovaz.kafkaonkafka.adapter.out.kafka;

import com.kiovaz.kafkaonkafka.domain.Quote;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class QuotePublisher {

    private static final String TOPIC = "quotes";

    private final KafkaTemplate<String, Quote> kafka;

    public QuotePublisher(KafkaTemplate<String, Quote> kafka) {
        this.kafka = kafka;
    }

    /** Sends the quote and waits for Kafka's acknowledgement; throws if it does not arrive. */
    public void publish(Quote quote) {
        try {
            kafka.send(TOPIC, quote.part(), quote).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing " + quote.id(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Could not publish " + quote.id(), e);
        }
    }
}
