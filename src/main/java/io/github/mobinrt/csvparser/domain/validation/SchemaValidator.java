package io.github.mobinrt.csvparser.domain.validation;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import io.github.mobinrt.csvparser.domain.model.ColumnDef;
import io.github.mobinrt.csvparser.domain.model.CsvFormat;
import io.github.mobinrt.csvparser.domain.model.Schema;

public final class SchemaValidator {

    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public void validate(Schema schema) {
        Objects.requireNonNull(schema, "schema");

        validateTableNames(schema);
        validateColumns(schema);
        validateCsvFormat(schema);
    }

    private void validateTableNames(Schema schema) {
        String tableName = trimToNull(schema.getTableName());
        requirePresent("schema.getTableName is required", tableName);
        requireValidIdentifier("tableName", tableName);
    }

    private void validateColumns(Schema schema) {
        if (schema.getColumns() == null || schema.getColumns().isEmpty()) {
            throw new SchemaValidationException("schema.getColumns must contain at least 1 column");
        }

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < schema.getColumns().size(); i++) {
            ColumnDef col = schema.getColumns().get(i);
            validateColumn(col, i, seen);
        }
    }

    private void validateColumn(ColumnDef col, int index, Set<String> seen) {
        if (col == null) {
            throw new SchemaValidationException("schema.getColumns contains null at index " + index);
        }

        String name = trimToNull(col.getName());
        requirePresent("Column name is required at index " + index, name);
        requireValidIdentifier("column[" + index + "].name", name);
        requireUniqueColumnName(name, seen);

        validateOptionalType(name, col.getType());
    }

    private void validateOptionalType(String columnName, String typeRaw) {
        String type = trimToNull(typeRaw);
        if (type == null) {
            return;
        }

        if (!looksLikeSqlType(type)) {
            throw new SchemaValidationException(
                    "Unsupported/invalid type for column '" + columnName + "': " + type
            );
        }
    }

    private void requireUniqueColumnName(String name, Set<String> seen) {
        String key = name.toLowerCase(Locale.ROOT);
        if (!seen.add(key)) {
            throw new SchemaValidationException("Duplicate column name: " + name);
        }
    }

    private void validateCsvFormat(Schema schema) {
        CsvFormat csv = schema.getCsv();
        if (csv == null) {
            throw new SchemaValidationException("schema.getCsv is required");
        }

        requireRealCharacter("csv.getDelimiter", csv.getDelimiter());
        requireRealCharacter("csv.getQuote", csv.getQuote());

        if (csv.getDelimiter() == csv.getQuote()) {
            throw new SchemaValidationException("csv.getDelimiter and csv.getQuote must be different characters");
        }
    }

    private void requireRealCharacter(String fieldName, char value) {
        if (value == '\0') {
            throw new SchemaValidationException(fieldName + " must be a real character");
        }
    }

    private static void requirePresent(String message, String value) {
        if (value == null) {
            throw new SchemaValidationException(message);
        }
    }

    private static void requireValidIdentifier(String field, String value) {
        if (!SQL_IDENTIFIER.matcher(value).matches()) {
            throw new SchemaValidationException(
                    "Invalid SQL identifier for " + field + ": '" + value + "'. "
                    + "Allowed: [A-Za-z_][A-Za-z0-9_]*"
            );
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean looksLikeSqlType(String type) {
        String t = type.trim().toUpperCase(Locale.ROOT);

        if (t.equals("INT") || t.equals("INTEGER") || t.equals("BIGINT")
                || t.equals("DATE") || t.equals("DATETIME") || t.equals("TIMESTAMP")
                || t.equals("BOOLEAN") || t.equals("TEXT")) {
            return true;
        }

        if (t.matches("VARCHAR\\(\\d{1,5}\\)")) {
            return true;
        }

        return t.matches("DECIMAL\\(\\d{1,3},\\d{1,3}\\)");
    }
}
