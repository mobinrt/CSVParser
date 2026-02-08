package io.github.mobinrt.csvparser.domain.model;

import java.util.List;
import java.util.Objects;

public final class Schema {

    private final String tableName;
    private final List<ColumnDef> columns;
    private final CsvFormat csv;

    public Schema(String tableName, List<ColumnDef> columns, CsvFormat csv) {
        this.tableName = tableName;
        this.columns = List.copyOf(Objects.requireNonNull(columns, "columns must not be null"));
        this.csv = Objects.requireNonNull(csv, "csv must not be null");
    }

    public String getTableName() {
        return tableName;
    }

    public List<ColumnDef> getColumns() {
        return columns;
    }

    public CsvFormat getCsv() {
        return csv;
    }
}
