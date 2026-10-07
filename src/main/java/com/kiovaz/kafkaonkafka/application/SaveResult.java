package com.kiovaz.kafkaonkafka.application;

import com.kiovaz.kafkaonkafka.adapter.out.persistence.ResultRepository;
import com.kiovaz.kafkaonkafka.domain.EmotionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SaveResult {

    private static final Logger log = LoggerFactory.getLogger(SaveResult.class);

    private final ResultRepository repository;

    public SaveResult(ResultRepository repository) {
        this.repository = repository;
    }

    public void handle(EmotionResult result) {
        if (repository.save(result)) {
            log.info("Saved {}", result.quoteId());
        } else {
            log.info("Already saved, ignored {}", result.quoteId());
        }
    }
}
