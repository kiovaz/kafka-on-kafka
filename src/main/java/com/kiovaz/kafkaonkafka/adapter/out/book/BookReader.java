package com.kiovaz.kafkaonkafka.adapter.out.book;

import com.kiovaz.kafkaonkafka.domain.Quote;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class BookReader {

    private final Path configPath;

    public BookReader(@Value("${app.book.config-path}") Path configPath) {
        this.configPath = configPath;
    }

    public List<Quote> read() {
        BookConfig config = readConfig();
        if (!"paragraph".equals(config.chunking())) {
            throw new IllegalStateException("Unsupported chunking '" + config.chunking() + "': only 'paragraph' is supported");
        }
        List<String> lines = readLines(configPath.resolveSibling(config.file()));
        int start = indexOf(lines, config.startMarker());
        int end = indexOf(lines, config.endMarker());
        Pattern section = Pattern.compile(config.sectionPattern());

        List<Quote> quotes = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        String part = null;
        for (String line : lines.subList(start + 1, end)) {
            if (line.isBlank()) {
                add(quotes, paragraph, config, part);
            } else if (section.matcher(line.trim()).find()) {
                add(quotes, paragraph, config, part);
                part = line.trim();
            } else {
                paragraph.append(line).append(' ');
            }
        }
        add(quotes, paragraph, config, part);
        return quotes;
    }

    private void add(List<Quote> quotes, StringBuilder paragraph, BookConfig config, String part) {
        String text = paragraph.toString().replaceAll("\\s+", " ").trim();
        paragraph.setLength(0);
        if (text.length() >= config.minLength()) {
            quotes.add(new Quote("q-%04d".formatted(quotes.size() + 1), text, config.title(), part));
        }
    }

    private BookConfig readConfig() {
        try {
            return JsonMapper.builder().build().readValue(Files.readString(configPath), BookConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read book config " + configPath + ": " + e.getMessage(), e);
        }
    }

    private List<String> readLines(Path text) {
        try {
            return Files.readAllLines(text);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read book text " + text + ": " + e.getMessage(), e);
        }
    }

    private int indexOf(List<String> lines, String marker) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(marker)) {
                return i;
            }
        }
        throw new IllegalStateException("Marker not found in the book text: " + marker);
    }
}
