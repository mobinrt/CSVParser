package io.github.mobinrt.csvparser.domain.errors;

public final class CsvRowReadException extends CsvReadException {

    private final long rowNumber;
    private final String rawRow;

    public CsvRowReadException(String sourceFile, long rowNumber, String rawRow, String message) {
        super(sourceFile, message);
        this.rowNumber = rowNumber;
        this.rawRow = rawRow;
    }

    public long getRowNumber() {
        return rowNumber;
    }

    public String getRawRow() {
        return rawRow;
    }
}
