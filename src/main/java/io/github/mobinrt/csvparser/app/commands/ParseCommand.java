package io.github.mobinrt.csvparser.app.commands;

import io.github.mobinrt.csvparser.usecase.ParseCsvUseCase;
import io.github.mobinrt.csvparser.usecase.ParseRequest;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Command(name = "parse", mixinStandardHelpOptions = true, description = "Parse CSV inputs and write to MySQL; bad rows go to error table.")
public final class ParseCommand implements Runnable {

    @Option(names = {"--schema"}, required = true, description = "Path to schema.json")
    private Path schemaPath;

    @Option(names = {"--input"}, required = true, description = "Input file or folder (repeatable)")
    private List<Path> inputs = new ArrayList<>();

    @Option(names = {"--recursive"}, description = "Recursively scan folders for CSV files")
    private boolean recursive = false;

    @Option(names = {"--db-url"}, description = "JDBC URL (or env CSV_DB_URL)")
    private String dbUrl;

    @Option(names = {"--db-user"}, description = "DB user (or env CSV_DB_USER)")
    private String dbUser;

    @Option(names = {"--db-pass"}, description = "DB password (or env CSV_DB_PASS)")
    private String dbPass;

    @Option(names = {"--table"}, description = "Override table name from schema")
    private String tableOverride;

    @Option(names = {
        "--include-columns"}, split = ",", description = "Comma-separated list of columns to insert (default: all)")
    private List<String> includeColumns = new ArrayList<>();

    @Option(names = {"--batch-size"}, defaultValue = "1000", description = "Batch insert size")
    private int batchSize;

    @Option(names = {"--validate-types"}, description = "Enable schema type validation; mismatches go to error table")
    private boolean validateTypes;

    @Option(names = {
        "--stop-on-fatal"}, defaultValue = "true", description = "Stop if DB connectivity/table creation fails")
    private boolean stopOnFatal;

    @Option(names = {
        "--max-errors"}, defaultValue = "-1", description = "Stop processing if error rows reach this limit (-1 = unlimited)")
    private long maxErrors;

    @Override
    public void run() {
        ParseRequest request = ParseRequest.builder()
                .schemaPath(schemaPath)
                .inputs(inputs)
                .recursive(recursive)
                .dbUrl(firstNonBlank(dbUrl, System.getenv("CSV_DB_URL")))
                .dbUser(firstNonBlank(dbUser, System.getenv("CSV_DB_USER")))
                .dbPass(firstNonBlank(dbPass, System.getenv("CSV_DB_PASS")))
                .tableOverride(tableOverride)
                .includeColumns(includeColumns)
                .batchSize(batchSize)
                .validateTypes(validateTypes)
                .stopOnFatal(stopOnFatal)
                .maxErrors(maxErrors)
                .build();

        new ParseCsvUseCase().execute(request);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
