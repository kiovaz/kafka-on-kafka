package com.kiovaz.kafkaonkafka.adapter.in.scheduler;

import com.kiovaz.kafkaonkafka.application.PublishBook;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.publisher.enabled", havingValue = "true", matchIfMissing = true)
class PublishBookJob {

    private final PublishBook publishBook;

    PublishBookJob(PublishBook publishBook) {
        this.publishBook = publishBook;
    }

    @Scheduled(fixedDelayString = "${app.publisher.delay-ms}")
    void run() {
        publishBook.publishNext();
    }
}
