package io.github.mobinrt.csvparser.domain.errors;

public final class CsvFileReadException extends CsvReadException {

    public CsvFileReadException(String sourceFile, String message, Throwable cause) {
        super(sourceFile, message, cause);
    }
}
