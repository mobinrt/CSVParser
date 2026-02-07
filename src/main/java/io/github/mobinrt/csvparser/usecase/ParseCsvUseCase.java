package io.github.mobinrt.csvparser.usecase;

import java.nio.file.Path;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mobinrt.csvparser.domain.model.ScanMode;
import io.github.mobinrt.csvparser.domain.model.Schema;
import io.github.mobinrt.csvparser.domain.ports.ErrorWriter;
import io.github.mobinrt.csvparser.domain.ports.InputResolver;
import io.github.mobinrt.csvparser.domain.ports.TableWriter;
import io.github.mobinrt.csvparser.domain.validation.SchemaValidator;
import io.github.mobinrt.csvparser.infrastructure.db.DataSourceFactory;
import io.github.mobinrt.csvparser.infrastructure.db.MySqlErrorWriter;
import io.github.mobinrt.csvparser.infrastructure.db.MySqlTableWriter;
import io.github.mobinrt.csvparser.infrastructure.filesystem.FileSystemInputResolver;
import io.github.mobinrt.csvparser.infrastructure.schema.JsonSchemaLoader;

/**
 * orchestrate schema loading, input discovery, CSV streaming, DB writes.
 */
public final class ParseCsvUseCase {

    private static final Logger log = LoggerFactory.getLogger(ParseCsvUseCase.class);

    private final JsonSchemaLoader schemaLoader = new JsonSchemaLoader();
    private final SchemaValidator schemaValidator = new SchemaValidator();
    private final InputResolver inputResolver = new FileSystemInputResolver();

    public void execute(ParseRequest request) {
        log.info("Starting parse run. schema={}, inputs={}, recursive={}, validateTypes={}, batchSize={}",
                request.schemaPath(), request.inputs(), request.recursive(), request.validateTypes(), request.batchSize());

        if (request.dbUrl() == null || request.dbUser() == null || request.dbPass() == null) {
            throw new IllegalArgumentException(
                    "DB config missing. Provide --db-url/--db-user/--db-pass or env CSV_DB_URL/CSV_DB_USER/CSV_DB_PASS"
            );
        }

        Schema schema = schemaSetup(request);

        inputSetup(request);

        dbSetup(request, schema);

    }

    private Schema schemaSetup(ParseRequest request) {
        Schema schema = schemaLoader.load(request.schemaPath());
        schemaValidator.validate(schema);

        log.info("Schema loaded and validated. tableName={}, columns={}", schema.getTableName(), schema.getColumns().size());
        return schema;
    }

    private void inputSetup(ParseRequest request) {
        ScanMode scanMode = request.recursive() ? ScanMode.RECURSIVE : ScanMode.NON_RECURSIVE;
        List<Path> csvFiles = inputResolver.resolveCsvFiles(request.inputs(), scanMode);
        if (csvFiles.isEmpty()) {
            throw new IllegalArgumentException("No CSV files found in the provided inputs.");
        }
        log.info("Resolved {} CSV file(s) to process.", csvFiles.size());
        if (csvFiles.size() <= 10) {
            for (Path p : csvFiles) {
                log.info("  - {}", p);
            }
        } else {
            for (int i = 0; i < 10; i++) {
                log.info("  - {}", csvFiles.get(i));
            }
            log.info("  ... ({} more)", csvFiles.size() - 10);
        }
    }

    private void dbSetup(ParseRequest request, Schema schema) {
        DataSource dataSource = new DataSourceFactory().createMySqlDataSource(request.dbUrl(), request.dbUser(), request.dbPass());
        ErrorWriter errorWriter = new MySqlErrorWriter(dataSource);
        TableWriter tableWriter = new MySqlTableWriter(dataSource);

        errorWriter.ensureErrorTableExist();
        tableWriter.ensureDataTableExists(schema, request.tableOverride(), request.includeColumns());
    }

}
