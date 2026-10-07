package com.kiovaz.kafkaonkafka.adapter.out.persistence;

import com.kiovaz.kafkaonkafka.domain.Emotion;
import com.kiovaz.kafkaonkafka.domain.EmotionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the real schema.sql and the real insert on H2 in PostgreSQL mode (the real PostgreSQL is checked at the end). */
class ResultRepositoryTest {

    private DriverManagerDataSource dataSource;
    private JdbcClient jdbc;
    private ResultRepository repository;

    @BeforeEach
    void setUp() {
        dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        runSchema();
        jdbc = JdbcClient.create(dataSource);
        repository = new ResultRepository(jdbc);
    }

    @Test
    void savesAResultWithTheEmotionInLowercase() {
        assertTrue(repository.save(result("The Metamorphosis", "q-0042", Emotion.DESPAIR, "I")));

        assertEquals("q-0042|despair|I", row());
    }

    @Test
    void keepsTheFirstResultWhenTheSameQuoteArrivesAgain() {
        assertTrue(repository.save(result("The Metamorphosis", "q-0042", Emotion.DESPAIR, "I")));
        assertFalse(repository.save(result("The Metamorphosis", "q-0042", Emotion.ALIENATION, "I")));

        assertEquals(1L, count());
        assertEquals("q-0042|despair|I", row());
    }

    @Test
    void theSameQuoteIdInAnotherBookIsAnotherRow() {
        repository.save(result("Book A", "q-0001", Emotion.ANXIETY, "I"));
        repository.save(result("Book B", "q-0001", Emotion.ANXIETY, "I"));

        assertEquals(2L, count());
    }

    @Test
    void savesAResultWithoutAPart() {
        assertTrue(repository.save(result("The Metamorphosis", "q-0001", Emotion.UNKNOWN, null)));

        assertEquals("q-0001|unknown|null", row());
    }

    @Test
    void runningTheSchemaAgainKeepsTheData() {
        repository.save(result("The Metamorphosis", "q-0042", Emotion.DESPAIR, "I"));

        runSchema();

        assertEquals(1L, count());
    }

    private void runSchema() {
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
    }

    private long count() {
        return jdbc.sql("SELECT count(*) FROM classified_quotes").query(Long.class).single();
    }

    private String row() {
        return jdbc.sql("SELECT quote_id, emotion, part FROM classified_quotes")
                .query((rs, n) -> rs.getString("quote_id") + "|" + rs.getString("emotion") + "|" + rs.getString("part"))
                .single();
    }

    private static EmotionResult result(String book, String quoteId, Emotion emotion, String part) {
        return new EmotionResult(quoteId, "some text", emotion, book, part, Instant.parse("2026-10-03T14:22:10Z"));
    }
}
