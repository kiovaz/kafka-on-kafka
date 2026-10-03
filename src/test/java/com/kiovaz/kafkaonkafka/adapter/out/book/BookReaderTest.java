package com.kiovaz.kafkaonkafka.adapter.out.book;

import com.kiovaz.kafkaonkafka.domain.Quote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookReaderTest {

    @TempDir
    Path tmp;

    @Test
    void readsTheFixtureBook() {
        List<Quote> quotes = new BookReader(Path.of("src/test/resources/book/book-config.json")).read();

        assertEquals(List.of(
                new Quote("q-0001", "This paragraph comes before any section marker.", "Test Book", null),
                new Quote("q-0002", "First paragraph of part one, wrapped over two lines here.", "Test Book", "I"),
                new Quote("q-0003", "Second paragraph of part one is long enough.", "Test Book", "I"),
                new Quote("q-0004", "Only paragraph of part two goes here.", "Test Book", "II")), quotes);
    }

    @Test
    void failsWhenAMarkerIsMissing() throws IOException {
        Path config = write("MISSING-START", "paragraph");
        var e = assertThrows(IllegalStateException.class, () -> new BookReader(config).read());
        assertTrue(e.getMessage().contains("MISSING-START"), e.getMessage());
    }

    @Test
    void failsOnUnsupportedChunking() throws IOException {
        Path config = write("*** START", "sentence");
        var e = assertThrows(IllegalStateException.class, () -> new BookReader(config).read());
        assertTrue(e.getMessage().contains("only 'paragraph'"), e.getMessage());
    }

    @Test
    void failsWhenTheConfigFileIsMissing() {
        assertThrows(IllegalStateException.class, () -> new BookReader(tmp.resolve("nope.json")).read());
    }

    @Test
    void readsTheRealBook() {
        List<Quote> quotes = new BookReader(Path.of("data/book-config.json")).read();

        Set<String> parts = quotes.stream().map(Quote::part).collect(Collectors.toSet());
        assertTrue(parts.containsAll(Set.of("I", "II", "III")), parts.toString());
        assertFalse(quotes.stream().anyMatch(q -> q.text().toLowerCase().contains("gutenberg")));
        System.out.println("REAL BOOK: " + quotes.size() + " quotes; first=" + quotes.getFirst()
                + "; last=" + quotes.getLast());
    }

    private Path write(String startMarker, String chunking) throws IOException {
        Files.writeString(tmp.resolve("book.txt"), "*** START\nsome text that is long enough\n*** END\n");
        Path config = tmp.resolve("book-config.json");
        Files.writeString(config, """
                {"title":"T","file":"book.txt","startMarker":"%s","endMarker":"*** END",
                 "sectionPattern":"^I$","chunking":"%s","minLength":5}
                """.formatted(startMarker, chunking));
        return config;
    }
}
