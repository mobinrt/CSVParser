package io.github.mobinrt.csvparser.usecase;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mobinrt.csvparser.domain.errors.CsvReadException;
import io.github.mobinrt.csvparser.domain.errors.CsvRowReadException;
import io.github.mobinrt.csvparser.domain.model.ColumnDef;
import io.github.mobinrt.csvparser.domain.model.ColumnSelection;
import io.github.mobinrt.csvparser.domain.model.ErrorRow;
import io.github.mobinrt.csvparser.domain.model.RowData;
import io.github.mobinrt.csvparser.domain.model.Schema;
import io.github.mobinrt.csvparser.domain.ports.CsvRowCursor;
import io.github.mobinrt.csvparser.domain.ports.CsvRowSource;
import io.github.mobinrt.csvparser.domain.ports.ErrorWriter;
import io.github.mobinrt.csvparser.domain.ports.InputResolver;
import io.github.mobinrt.csvparser.domain.ports.TableWriter;
import io.github.mobinrt.csvparser.domain.services.ColumnSelector;
import io.github.mobinrt.csvparser.domain.services.ScanMode;
import io.github.mobinrt.csvparser.domain.validation.NoOpRowTransformer;
import io.github.mobinrt.csvparser.domain.validation.RowTransformResult;
import io.github.mobinrt.csvparser.domain.validation.RowTransformer;
import io.github.mobinrt.csvparser.domain.validation.SchemaValidator;
import io.github.mobinrt.csvparser.domain.validation.TypedRowTransformer;
import io.github.mobinrt.csvparser.infrastructure.csv.CommonsCsvRowSource;
import io.github.mobinrt.csvparser.infrastructure.db.DataSourceFactory;
import io.github.mobinrt.csvparser.infrastructure.db.MySqlBatchInserter;
import io.github.mobinrt.csvparser.infrastructure.db.MySqlErrorWriter;
import io.github.mobinrt.csvparser.infrastructure.db.MySqlTableWriter;
import io.github.mobinrt.csvparser.infrastructure.db.SqlIdentifier;
import io.github.mobinrt.csvparser.infrastructure.filesystem.FileSystemInputResolver;
import io.github.mobinrt.csvparser.infrastructure.schema.JsonSchemaLoader;

/**
 * Orchestrates schema loading, input discovery, CSV streaming, validation, and
 * DB writes.
 */
public final class ParseCsvUseCase {

    private static final Logger log = LoggerFactory.getLogger(ParseCsvUseCase.class);

    private final JsonSchemaLoader schemaLoader = new JsonSchemaLoader();
    private final SchemaValidator schemaValidator = new SchemaValidator();
    private final InputResolver inputResolver = new FileSystemInputResolver();
    private final CsvRowSource csvRowSource = new CommonsCsvRowSource();

    public void execute(ParseRequest request) {
        log.info("Starting parse run. schema={}, inputs={}, recursive={}, validateTypes={}, batchSize={}",
                request.schemaPath(), request.inputs(), request.recursive(), request.validateTypes(), request.batchSize());

        requireDbConfig(request);

        Schema schema = schemaSetup(request);
        List<Path> csvFiles = inputSetup(request);

        DbContext db = dbSetup(request, schema);

        ColumnSelection selection = ColumnSelector.selectColumns(schema, request.includeColumns());
        RowTransformer transformer = createRowTransformer(request, selection);

        String targetTable = chooseTableName(schema, request.tableOverride());
        String insertSql = buildInsertSql(targetTable, selection.getSelectedColumns());

        ImportStats stats = runImport(request, schema, csvFiles, db, transformer, insertSql);

        log.info("Import finished. okRows={}, insertedRows={}, errorRows={}",
                stats.okRows, stats.insertedRows, stats.errorRows);
    }

    private RowTransformer createRowTransformer(ParseRequest request, ColumnSelection selection) {
        if (request.validateTypes()) {
            return new TypedRowTransformer(selection.getSelectedColumns(), selection.getSelectedIndices());
        }
        return new NoOpRowTransformer(selection.getSelectedIndices());
    }

    private Schema schemaSetup(ParseRequest request) {
        Schema schema = schemaLoader.load(request.schemaPath());
        schemaValidator.validate(schema);

        log.info("Schema loaded and validated. tableName={}, columns={}",
                schema.getTableName(), schema.getColumns().size());
        return schema;
    }

    private List<Path> inputSetup(ParseRequest request) {
        ScanMode scanMode = request.recursive() ? ScanMode.RECURSIVE : ScanMode.NON_RECURSIVE;

        List<Path> csvFiles = inputResolver.resolveCsvFiles(request.inputs(), scanMode);
        if (csvFiles.isEmpty()) {
            throw new IllegalArgumentException("No CSV files found in the provided inputs.");
        }

        log.info("Resolved {} CSV file(s) to process.", csvFiles.size());
        logResolvedFiles(csvFiles);

        return csvFiles;
    }

    private void logResolvedFiles(List<Path> csvFiles) {
        int max = Math.min(csvFiles.size(), 10);
        for (int i = 0; i < max; i++) {
            log.info("  - {}", csvFiles.get(i));
        }
        if (csvFiles.size() > 10) {
            log.info("  ... ({} more)", csvFiles.size() - 10);
        }
    }

    private DbContext dbSetup(ParseRequest request, Schema schema) {
        DataSource dataSource = new DataSourceFactory()
                .createMySqlDataSource(request.dbUrl(), request.dbUser(), request.dbPass());

        ErrorWriter errorWriter = new MySqlErrorWriter(dataSource);
        TableWriter tableWriter = new MySqlTableWriter(dataSource);

        errorWriter.ensureErrorTableExist();
        tableWriter.ensureDataTableExists(schema, request.tableOverride(), request.includeColumns());

        return new DbContext(dataSource, errorWriter, tableWriter, new MySqlBatchInserter());
    }

    private ImportStats runImport(
            ParseRequest request,
            Schema schema,
            List<Path> csvFiles,
            DbContext db,
            RowTransformer transformer,
            String insertSql
    ) {
        ImportStats stats = new ImportStats();
        int batchSize = request.batchSize();

        try (Connection connection = db.dataSource.getConnection()) {
            connection.setAutoCommit(false);

            for (Path csvFile : csvFiles) {
                processSingleFile(connection, csvFile, schema, db, transformer, insertSql, batchSize, stats);
            }

        } catch (Exception e) {
            throw new IllegalStateException("Fatal DB failure during import: " + e.getMessage(), e);
        }

        return stats;
    }

    private void processSingleFile(
            Connection connection,
            Path csvFile,
            Schema schema,
            DbContext db,
            RowTransformer transformer,
            String insertSql,
            int batchSize,
            ImportStats stats
    ) {
        log.info("Processing file: {}", csvFile);

        List<MySqlBatchInserter.InsertItem> batch = new ArrayList<>(batchSize);

        try (CsvRowCursor cursor = csvRowSource.openCursor(csvFile, schema)) {
            while (cursor.hasNext()) {
                try {
                    RowData row = cursor.next();
                    handleRow(connection, db, transformer, insertSql, batch, batchSize, stats, row);
                } catch (CsvRowReadException rowEx) {
                    stats.errorRows++;
                    writeRowError(db, rowEx.getSourceFile(), rowEx.getRowNumber(), rowEx.getRawRow(), rowEx.getMessage());
                }
            }

            flushAndCommitIfNeeded(connection, db, insertSql, batch, stats);

        } catch (CsvReadException fileEx) {
            stats.errorRows++;
            writeFileError(db, fileEx.getSourceFile(), fileEx.getMessage());
            log.warn("Skipping file due to CSV error: {} ({})", csvFile, fileEx.getMessage());
        } catch (Exception e) {
            throw new IllegalStateException("Fatal failure while processing file: " + csvFile + " (" + e.getMessage() + ")", e);
        }
    }

    private void handleRow(
            Connection connection,
            DbContext db,
            RowTransformer transformer,
            String insertSql,
            List<MySqlBatchInserter.InsertItem> batch,
            int batchSize,
            ImportStats stats,
            RowData row
    ) throws Exception {

        RowTransformResult result = transformer.transform(row);

        if (result instanceof RowTransformResult.Ok ok) {
            stats.okRows++;
            batch.add(new MySqlBatchInserter.InsertItem(row, ok.getJdbcValues()));

            if (batch.size() >= batchSize) {
                flushAndCommit(connection, db, insertSql, batch, stats);
            }
            return;
        }

        if (result instanceof RowTransformResult.Error err) {
            stats.errorRows++;
            writeRowError(db, row.getSourceFile(), row.getRowNumber(), row.getRawRow(),
                    "Type/format validation failed: " + err.getMessage());
            return;
        }

        stats.errorRows++;
        writeRowError(db, row.getSourceFile(), row.getRowNumber(), row.getRawRow(),
                "Unknown transform result type: " + result.getClass().getName());
    }

    private void flushAndCommitIfNeeded(
            Connection connection,
            DbContext db,
            String insertSql,
            List<MySqlBatchInserter.InsertItem> batch,
            ImportStats stats
    ) throws Exception {
        if (!batch.isEmpty()) {
            flushAndCommit(connection, db, insertSql, batch, stats);
        }
    }

    private void flushAndCommit(
            Connection connection,
            DbContext db,
            String insertSql,
            List<MySqlBatchInserter.InsertItem> batch,
            ImportStats stats
    ) throws Exception {
        stats.insertedRows += flushBatch(connection, db, insertSql, batch);
        connection.commit();
        batch.clear();
    }

    private void writeFileError(DbContext db, String sourceFile, String message) {
        db.errorWriter.writeErrorRow(new ErrorRow(sourceFile, null, null, message));
    }

    private void writeRowError(DbContext db, String sourceFile, long rowNumber, String rawRow, String message) {
        db.errorWriter.writeErrorRow(new ErrorRow(sourceFile, rowNumber, rawRow, message));
    }

    private long flushBatch(
            Connection connection,
            DbContext db,
            String insertSql,
            List<MySqlBatchInserter.InsertItem> batch
    ) {
        return db.batchInserter.insertWithUpdateCountsOptimization(connection, insertSql, batch, db.errorWriter);
    }

    private String chooseTableName(Schema schema, String tableOverride) {
        if (tableOverride != null && !tableOverride.isBlank()) {
            return tableOverride.trim();
        }
        return schema.getTableName();
    }

    private String buildInsertSql(String tableName, List<ColumnDef> columns) {
        SqlIdentifier.requireSafe("table name", tableName);

        StringBuilder colList = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                colList.append(",");
            }
            colList.append(SqlIdentifier.quote(columns.get(i).getName()));
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                placeholders.append(",");
            }
            placeholders.append("?");
        }

        return "INSERT INTO " + SqlIdentifier.quote(tableName)
                + " (" + colList + ") VALUES (" + placeholders + ")";
    }

    private void requireDbConfig(ParseRequest request) {
        if (request.dbUrl() == null || request.dbUser() == null || request.dbPass() == null) {
            throw new IllegalArgumentException(
                    "DB config missing. Provide --db-url/--db-user/--db-pass or env CSV_DB_URL/CSV_DB_USER/CSV_DB_PASS"
            );
        }
    }

    private static final class ImportStats {

        private long okRows;
        private long errorRows;
        private long insertedRows;
    }

    private static final class DbContext {

        private final DataSource dataSource;
        private final ErrorWriter errorWriter;
        @SuppressWarnings("unused")
        private final TableWriter tableWriter;
        private final MySqlBatchInserter batchInserter;

        private DbContext(
                DataSource dataSource,
                ErrorWriter errorWriter,
                TableWriter tableWriter,
                MySqlBatchInserter batchInserter
        ) {
            this.dataSource = dataSource;
            this.errorWriter = errorWriter;
            this.tableWriter = tableWriter;
            this.batchInserter = batchInserter;
        }
    }
}
