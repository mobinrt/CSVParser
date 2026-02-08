package io.github.mobinrt.csvparser.domain.errors;

public final class CsvFileFormatException extends CsvReadException {

    public CsvFileFormatException(String sourceFile, String message) {
        super(sourceFile, message);
    }
}
