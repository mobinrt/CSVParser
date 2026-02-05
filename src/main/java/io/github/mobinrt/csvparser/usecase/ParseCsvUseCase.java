package io.github.mobinrt.csvparser.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mobinrt.csvparser.domain.model.Schema;
import io.github.mobinrt.csvparser.domain.validation.SchemaValidator;
import io.github.mobinrt.csvparser.infrastructure.schema.JsonSchemaLoader;

/**
 * orchestrate schema loading, input discovery, CSV streaming, DB writes.
 */
public final class ParseCsvUseCase {

    private static final Logger log = LoggerFactory.getLogger(ParseCsvUseCase.class);

    private final JsonSchemaLoader schemaLoader = new JsonSchemaLoader();
    private final SchemaValidator schemaValidator = new SchemaValidator();

    public void execute(ParseRequest request) {
        log.info("Starting parse run. schema={}, inputs={}, recursive={}, validateTypes={}, batchSize={}",
                request.schemaPath(), request.inputs(), request.recursive(), request.validateTypes(), request.batchSize());

        if (request.dbUrl() == null || request.dbUser() == null || request.dbPass() == null) {
            throw new IllegalArgumentException(
                    "DB config missing. Provide --db-url/--db-user/--db-pass or env CSV_DB_URL/CSV_DB_USER/CSV_DB_PASS"
            );
        }

        Schema schema = schemaLoader.load(request.schemaPath());
        schemaValidator.validate(schema);

        log.info("Schema loaded and validated. tableName={}, columns={}", schema.getTableName(), schema.getColumns().size());

    }
}
