package io.github.mobinrt.csvparser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import io.github.mobinrt.csvparser.domain.model.ColumnDef;
import io.github.mobinrt.csvparser.domain.model.CsvFormat;
import io.github.mobinrt.csvparser.domain.model.Schema;
import io.github.mobinrt.csvparser.domain.validation.SchemaValidationException;
import io.github.mobinrt.csvparser.domain.validation.SchemaValidator;

class SchemaValidatorTest {

    private final SchemaValidator validator = new SchemaValidator();

    @Test
    void validSchema_passes() {
        Schema s = new Schema(
                "import_data",
                List.of(
                        new ColumnDef("id", "INT"),
                        new ColumnDef("name", "VARCHAR(255)"),
                        new ColumnDef("created_at", "DATETIME")
                ),
                new CsvFormat(',', '"', true)
        );

        assertDoesNotThrow(() -> validator.validate(s));
    }

    @Test
    void duplicateColumns_fails() {
        Schema s = new Schema(
                "t",
                List.of(new ColumnDef("id", "INT"), new ColumnDef("id", "INT")),
                new CsvFormat(',', '"', true)
        );

        SchemaValidationException ex = assertThrows(SchemaValidationException.class, () -> validator.validate(s));
        assertTrue(ex.getMessage().toLowerCase().contains("duplicate"));
    }

    @Test
    void invalidIdentifier_fails() {
        Schema s = new Schema(
                "bad-table-name",
                List.of(new ColumnDef("id", "INT")),
                new CsvFormat(',', '"', true)
        );

        SchemaValidationException ex = assertThrows(SchemaValidationException.class, () -> validator.validate(s));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid sql identifier"));
    }
}
