package io.github.mobinrt.csvparser.infrastructure.db;

import java.util.regex.Pattern;

public final class SqlIdentifier {

    private static final Pattern SAFE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private SqlIdentifier() {
    }

    public static void requireSafe(String what, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(what + " is required");
        }
        if (!SAFE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier for " + what + ": '" + value
                    + "'. Allowed: [A-Za-z_][A-Za-z0-9_]*");
        }
    }

    public static String quote(String identifier) {
        requireSafe("identifier", identifier);
        return "`" + identifier + "`";
    }
}
