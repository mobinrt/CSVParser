package io.github.mobinrt.csvparser.domain.validation;

import java.util.Objects;

public sealed interface RowTransformResult permits RowTransformResult.Ok, RowTransformResult.Error {

    static Ok ok(Object[] jdbcValues) {
        return new Ok(jdbcValues);
    }

    static Error error(String message) {
        return new Error(message);
    }

    final class Ok implements RowTransformResult {

        private final Object[] jdbcValues;

        public Ok(Object[] jdbcValues) {
            this.jdbcValues = Objects.requireNonNull(jdbcValues, "jdbcValues").clone();
        }

        public Object[] getJdbcValues() {
            return jdbcValues.clone();
        }
    }

    final class Error implements RowTransformResult {

        private final String message;

        public Error(String message) {
            this.message = Objects.requireNonNull(message, "message");
        }

        public String getMessage() {
            return message;
        }
    }

}
