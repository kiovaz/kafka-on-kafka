package com.kiovaz.kafkaonkafka.adapter.out.kafka;

import com.kiovaz.kafkaonkafka.domain.EmotionResult;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class EmotionResultPublisher {

    private static final String TOPIC = "emotions";

    private final KafkaTemplate<String, EmotionResult> kafka;

    public EmotionResultPublisher(KafkaTemplate<String, EmotionResult> kafka) {
        this.kafka = kafka;
    }

    /** Sends the result and waits for Kafka's acknowledgement; throws if it does not arrive. */
    public void publish(EmotionResult result) {
        try {
            kafka.send(TOPIC, result.part(), result).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing the result of " + result.quoteId(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Could not publish the result of " + result.quoteId(), e);
        }
    }
}
