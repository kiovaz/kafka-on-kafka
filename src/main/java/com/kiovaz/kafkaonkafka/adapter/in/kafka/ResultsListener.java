package com.kiovaz.kafkaonkafka.adapter.in.kafka;

import com.kiovaz.kafkaonkafka.application.SaveResult;
import com.kiovaz.kafkaonkafka.domain.EmotionResult;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class ResultsListener {

    private final SaveResult saveResult;

    ResultsListener(SaveResult saveResult) {
        this.saveResult = saveResult;
    }

    @KafkaListener(topics = "emotions", groupId = "results-writer",
            properties = "spring.json.value.default.type=com.kiovaz.kafkaonkafka.domain.EmotionResult")
    void on(EmotionResult result) {
        saveResult.handle(result);
    }
}
