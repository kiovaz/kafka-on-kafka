## Purpose

Defines how the book's text file and its config become the list of quotes the publisher sends, so changing books needs only a new text file and config.

## ADDED Requirements

### Requirement: Quotes from the book text
The reader SHALL read the text file named in `book-config.json` and return a list of quotes in book order. Only the text between the start and end marker lines SHALL be used. The text SHALL be split into paragraphs at blank lines, each paragraph SHALL have its line breaks and repeated whitespace replaced by single spaces and be trimmed, and paragraphs shorter than `minLength` characters after that SHALL be discarded.

#### Scenario: Header and footer excluded
- **WHEN** the text has a license header before the start marker and a license footer after the end marker
- **THEN** no quote contains text from the header or the footer

#### Scenario: Wrapped lines joined
- **WHEN** a paragraph is written over several lines
- **THEN** the quote is one line of text with single spaces where the line breaks were

#### Scenario: Short paragraphs discarded
- **WHEN** a paragraph is shorter than `minLength` characters
- **THEN** it does not become a quote

### Requirement: Sections
A line that matches `sectionPattern` SHALL start a new section, SHALL NOT become a quote, and SHALL give its text as the `part` of every following quote until the next section line. Quotes before the first section line SHALL have a null `part`.

#### Scenario: Part from the section line
- **WHEN** a section line `II` is followed by two paragraphs
- **THEN** both quotes have `part` `II`

#### Scenario: No section yet
- **WHEN** a paragraph comes before any section line
- **THEN** its quote has a null `part`

### Requirement: Quote identity
Each quote SHALL have the `book` of the config's `title` and an `id` of the form `q-0001`, numbered in book order starting at 1 with at least four digits.

#### Scenario: Sequential ids
- **WHEN** the book yields three quotes
- **THEN** their ids are `q-0001`, `q-0002` and `q-0003` in book order

### Requirement: Invalid configuration fails clearly
When reading cannot proceed, the application SHALL fail to start with a message that names the problem. This includes a missing config file, a missing text file, a start or end marker that is not found in the text, and a `chunking` value other than `paragraph`.

#### Scenario: Missing marker
- **WHEN** the `startMarker` does not appear in the text file
- **THEN** reading fails with a message naming the marker

#### Scenario: Unsupported chunking
- **WHEN** the config has `chunking` set to `sentence`
- **THEN** reading fails with a message saying only `paragraph` is supported

### Requirement: The shipped book reads sensibly
Reading the shipped *The Metamorphosis* SHALL produce quotes from all three parts `I`, `II` and `III`, and no quote SHALL contain text from the Project Gutenberg header or footer.

#### Scenario: Real data
- **WHEN** the shipped `data/book-config.json` is read
- **THEN** the quotes include parts `I`, `II` and `III`
- **AND** no quote contains the words "Project Gutenberg"
