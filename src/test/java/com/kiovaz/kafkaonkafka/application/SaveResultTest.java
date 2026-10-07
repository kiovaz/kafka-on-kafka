package com.kiovaz.kafkaonkafka.application;

import com.kiovaz.kafkaonkafka.adapter.out.persistence.ResultRepository;
import com.kiovaz.kafkaonkafka.domain.Emotion;
import com.kiovaz.kafkaonkafka.domain.EmotionResult;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaveResultTest {

    private final ResultRepository repository = mock(ResultRepository.class);
    private final EmotionResult result =
            new EmotionResult("q-0042", "text", Emotion.DESPAIR, "The Metamorphosis", "I", Instant.parse("2026-10-03T14:22:10Z"));

    @Test
    void savesTheResultOnce() {
        when(repository.save(result)).thenReturn(true);

        new SaveResult(repository).handle(result);

        verify(repository, times(1)).save(result);
    }

    @Test
    void aDuplicateThatIsIgnoredIsNotAnError() {
        when(repository.save(result)).thenReturn(false);

        assertDoesNotThrow(() -> new SaveResult(repository).handle(result));
    }

    @Test
    void aDatabaseFailureReachesTheCallerSoTheMessageIsRetried() {
        when(repository.save(result)).thenThrow(new DataAccessResourceFailureException("database down"));

        assertThrows(DataAccessResourceFailureException.class, () -> new SaveResult(repository).handle(result));
    }
}
