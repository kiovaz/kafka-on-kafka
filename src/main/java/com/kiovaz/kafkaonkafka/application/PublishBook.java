package com.kiovaz.kafkaonkafka.application;

import com.kiovaz.kafkaonkafka.adapter.out.book.BookReader;
import com.kiovaz.kafkaonkafka.adapter.out.kafka.QuotePublisher;
import com.kiovaz.kafkaonkafka.domain.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "app.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class PublishBook {

    private static final Logger log = LoggerFactory.getLogger(PublishBook.class);

    private final QuotePublisher publisher;
    private final List<Quote> quotes;
    private int next = 0;

    public PublishBook(BookReader reader, QuotePublisher publisher) {
        this.publisher = publisher;
        this.quotes = reader.read();
        log.info("Book loaded: {} quotes to publish", quotes.size());
    }

    /** Publishes the next quote; it only moves on after Kafka acknowledged it, so a failure retries the same one. */
    public void publishNext() {
        if (next >= quotes.size()) {
            return;
        }
        Quote quote = quotes.get(next);
        publisher.publish(quote);
        next++;
        log.info("Published {} ({}/{})", quote.id(), next, quotes.size());
        if (next == quotes.size()) {
            log.info("Finished publishing the book");
        }
    }
}
