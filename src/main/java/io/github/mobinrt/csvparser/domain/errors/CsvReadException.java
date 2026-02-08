package io.github.mobinrt.csvparser.domain.errors;

public class CsvReadException extends RuntimeException {

    private final String sourceFile;

    public CsvReadException(String sourceFile, String message) {
        super(message);
        this.sourceFile = sourceFile;
    }

    public CsvReadException(String sourceFile, String message, Throwable cause) {
        super(message, cause);
        this.sourceFile = sourceFile;
    }

    public String getSourceFile() {
        return sourceFile;
    }
}
