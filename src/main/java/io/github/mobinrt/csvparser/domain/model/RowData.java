package io.github.mobinrt.csvparser.domain.model;

import java.util.List;
import java.util.Objects;

public final class RowData {

    private final String sourceFile;
    private final long rowNumber;
    private final List<String> values;
    private final String rawRow;

    public RowData(String sourceFile, long rowNumber, List<String> values, String rawRow) {
        this.sourceFile = Objects.requireNonNull(sourceFile, "sourceFile must not be null");
        this.rowNumber = rowNumber;
        this.values = List.copyOf(Objects.requireNonNull(values, "values must not be null"));
        this.rawRow = rawRow;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public long getRowNumber() {
        return rowNumber;
    }

    public List<String> getValues() {
        return values;
    }

    public String getRawRow() {
        return rawRow;
    }

}
