package com.kiovaz.kafkaonkafka.adapter.in.kafka;

import com.kiovaz.kafkaonkafka.application.ClassifyQuote;
import com.kiovaz.kafkaonkafka.domain.Quote;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class QuoteListener {

    private final ClassifyQuote classifyQuote;

    QuoteListener(ClassifyQuote classifyQuote) {
        this.classifyQuote = classifyQuote;
    }

    @KafkaListener(topics = "quotes", groupId = "classifier", properties = {
            "max.poll.records=1",
            "spring.json.value.default.type=com.kiovaz.kafkaonkafka.domain.Quote"})
    void on(Quote quote) {
        classifyQuote.handle(quote);
    }
}
