package io.github.mobinrt.csvparser.domain.validation;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ValueConverter {

    private static final DateTimeFormatter DT_1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DT_2 = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter D_1 = DateTimeFormatter.ISO_LOCAL_DATE;

    public Object coerce(String schemaType, String rawValue) {
        String v = normalizeEmptyToNull(rawValue);
        if (v == null) {
            return null;
        }

        String t = normalizeType(schemaType);

        if (t.startsWith("VARCHAR(")) {
            int maxLen = parseSingleIntArg(t, "VARCHAR");
            if (v.length() > maxLen) {
                throw new IllegalArgumentException("Value too long for " + t + " (len=" + v.length() + ", max=" + maxLen + ")");
            }
            return v;
        }

        if (t.equals("TEXT")) {
            return v;
        }

        if (t.equals("INT") || t.equals("INTEGER")) {
            return Integer.valueOf(v);
        }
        if (t.equals("BIGINT")) {
            return Long.valueOf(v);
        }

        if (t.startsWith("DECIMAL(")) {
            return new BigDecimal(v);
        }

        if (t.equals("BOOLEAN")) {
            return parseBoolean(v);
        }

        if (t.equals("DATE")) {
            LocalDate d = LocalDate.parse(v, D_1);
            return Date.valueOf(d);
        }

        if (t.equals("DATETIME") || t.equals("TIMESTAMP")) {
            LocalDateTime dt = tryParseDateTime(v);
            return Timestamp.valueOf(dt);
        }

        return v;
    }

    private String normalizeType(String schemaType) {
        if (schemaType == null || schemaType.isBlank()) {
            return "TEXT";
        }
        return schemaType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmptyToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private Boolean parseBoolean(String v) {
        String t = v.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "true", "1", "yes", "y" ->
                Boolean.TRUE;
            case "false", "0", "no", "n" ->
                Boolean.FALSE;
            default ->
                throw new IllegalArgumentException("Invalid BOOLEAN value: " + v);
        };
    }

    private LocalDateTime tryParseDateTime(String v) {
        try {
            return LocalDateTime.parse(v, DT_1);
        } catch (RuntimeException ignored) {
        }
        try {
            return LocalDateTime.parse(v, DT_2);
        } catch (RuntimeException ignored) {
        }
        throw new IllegalArgumentException("Invalid DATETIME/TIMESTAMP value: " + v);
    }

    private int parseSingleIntArg(String type, String name) {
        int open = type.indexOf('(');
        int close = type.indexOf(')');
        if (open < 0 || close < 0 || close <= open + 1) {
            throw new IllegalArgumentException("Invalid " + name + " syntax: " + type);
        }
        String inside = type.substring(open + 1, close).trim();
        return Integer.parseInt(inside);
    }
}
