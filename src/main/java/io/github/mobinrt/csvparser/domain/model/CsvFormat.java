package io.github.mobinrt.csvparser.domain.model;

public final class CsvFormat {

    private final char delimiter;
    private final char quote;
    private final boolean hasHeader;

    public CsvFormat(char delimiter, char quote, boolean hasHeader) {
        this.delimiter = delimiter;
        this.quote = quote;
        this.hasHeader = hasHeader;
    }

    public char getDelimiter() {
        return delimiter;
    }

    public char getQuote() {
        return quote;
    }

    public boolean hasHeader() {
        return hasHeader;
    }
}
