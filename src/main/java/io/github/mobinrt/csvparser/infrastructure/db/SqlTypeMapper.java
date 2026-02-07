package io.github.mobinrt.csvparser.infrastructure.db;

import java.util.Locale;

public final class SqlTypeMapper {

    /**
     * Map schema type string to MySQL column type. If null/blank => default to
     * TEXT.
     */
    public String toMySqlType(String schemaType) {
        if (schemaType == null || schemaType.isBlank()) {
            return "TEXT";
        }
        String t = schemaType.trim().toUpperCase(Locale.ROOT);

        if (t.equals("INTEGER")) {
            return "INT";
        }

        return t;
    }
}
