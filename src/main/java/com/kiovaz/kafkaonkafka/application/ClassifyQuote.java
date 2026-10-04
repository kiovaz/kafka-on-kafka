package com.kiovaz.kafkaonkafka.application;

import com.kiovaz.kafkaonkafka.adapter.out.kafka.EmotionResultPublisher;
import com.kiovaz.kafkaonkafka.adapter.out.ollama.EmotionClassifier;
import com.kiovaz.kafkaonkafka.domain.Emotion;
import com.kiovaz.kafkaonkafka.domain.EmotionResult;
import com.kiovaz.kafkaonkafka.domain.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ClassifyQuote {

    private static final Logger log = LoggerFactory.getLogger(ClassifyQuote.class);

    private final EmotionClassifier classifier;
    private final EmotionResultPublisher publisher;

    public ClassifyQuote(EmotionClassifier classifier, EmotionResultPublisher publisher) {
        this.classifier = classifier;
        this.publisher = publisher;
    }

    public void handle(Quote quote) {
        Emotion emotion = classifier.classify(quote.text());
        publisher.publish(new EmotionResult(quote.id(), quote.text(), emotion, quote.book(), quote.part(), Instant.now()));
        log.info("Classified {} as {}", quote.id(), emotion.json());
    }
}
