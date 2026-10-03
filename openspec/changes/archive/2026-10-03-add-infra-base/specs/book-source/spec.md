## Purpose

Provides the book text and a config file describing how to read it, so the producer can switch books without code changes.

## ADDED Requirements

### Requirement: Book text file
The repository SHALL contain the plain-text *The Metamorphosis* from Project Gutenberg at `data/metamorphosis.txt`, including its Gutenberg start and end marker lines.

#### Scenario: Markers present
- **WHEN** `data/metamorphosis.txt` is searched for the configured start and end markers
- **THEN** both are found, the start marker before the end marker

### Requirement: Book configuration
The repository SHALL contain `data/book-config.json` with the fields `title`, `file`, `startMarker`, `endMarker`, `sectionPattern`, `chunking` and `minLength`, valid JSON, and consistent with the text file.

#### Scenario: Config describes the shipped book
- **WHEN** `data/book-config.json` is parsed
- **THEN** `file` names an existing file under `data/`
- **AND** `startMarker` and `endMarker` each match a line in that file
- **AND** `sectionPattern` is a valid regular expression that matches at least one line between the markers
- **AND** `chunking` is `paragraph` and `minLength` is a positive integer
