package io.github.mobinrt.csvparser.domain.model;

import java.util.Objects;

public final class ErrorRow {

    private final String sourceFile;
    private final Long rowNumber;
    private final String rawRow;
    private final String errorMessage;

    public ErrorRow(String sourceFile, Long rowNumber, String rawRow, String errorMessage) {
        this.sourceFile = Objects.requireNonNull(sourceFile, "sourceFile");
        this.rowNumber = rowNumber;
        this.rawRow = rawRow;
        this.errorMessage = Objects.requireNonNull(errorMessage, "errorMessage");
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public Long getRowNumber() {
        return rowNumber;
    }

    public String getRawRow() {
        return rawRow;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
