package io.github.mobinrt.csvparser.infrastructure.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import io.github.mobinrt.csvparser.domain.errors.CsvFileFormatException;
import io.github.mobinrt.csvparser.domain.errors.CsvFileReadException;
import io.github.mobinrt.csvparser.domain.errors.CsvRowReadException;
import io.github.mobinrt.csvparser.domain.model.ColumnDef;
import io.github.mobinrt.csvparser.domain.model.RowData;
import io.github.mobinrt.csvparser.domain.model.Schema;
import io.github.mobinrt.csvparser.domain.ports.CsvRowCursor;
import io.github.mobinrt.csvparser.domain.ports.CsvRowSource;

public final class CommonsCsvRowSource implements CsvRowSource {

    @Override
    public CsvRowCursor openCursor(Path csvFile, Schema schema) {
        Objects.requireNonNull(csvFile, "csvFile must not be null");
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(schema.getCsv(), "schema.csv must not be null");

        String sourceFile = csvFile.toAbsolutePath().normalize().toString();
        CSVParser parser = openParser(csvFile, schema, sourceFile);

        try {
            if (schema.getCsv().hasHeader()) {
                validateHeader(parser, schema, sourceFile);
            }
            return createCursor(csvFile, schema, parser, sourceFile);
        } catch (RuntimeException e) {
            closeQuietly(parser);
            throw e;
        }
    }

    private CSVParser openParser(Path csvFile, Schema schema, String sourceFile) {
        try {
            BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
            CSVFormat format = buildFormat(schema);
            return new CSVParser(reader, format);
        } catch (IOException e) {
            throw new CsvFileReadException(sourceFile, "Failed to open/read CSV file", e);
        }
    }

    private CSVFormat buildFormat(Schema schema) {
        var csv = schema.getCsv();

        return CSVFormat.DEFAULT.builder()
                .setDelimiter(csv.getDelimiter())
                .setQuote(csv.getQuote())
                .setTrim(true)
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreEmptyLines(true)
                .setAllowMissingColumnNames(false)
                .setLenientEof(true)
                .setHeader(csv.hasHeader() ? new String[0] : null)
                .setSkipHeaderRecord(csv.hasHeader())
                .build();
    }

    private CsvRowCursor createCursor(Path csvFile, Schema schema, CSVParser parser, String sourceFile) {
        int expectedColumns = schema.getColumns().size();
        char delimiter = schema.getCsv().getDelimiter();

        Iterator<CSVRecord> iterator = parser.iterator();
        return new CommonsCursor(csvFile, parser, iterator, expectedColumns, delimiter, sourceFile);
    }

    private void validateHeader(CSVParser parser, Schema schema, String sourceFile) {
        List<String> actualHeader = parser.getHeaderNames();
        List<String> expectedHeader = schema.getColumns().stream()
                .map(ColumnDef::getName)
                .toList();

        ensureSameHeaderSize(expectedHeader, actualHeader, sourceFile);
        ensureSameHeaderNames(expectedHeader, actualHeader, sourceFile);
    }

    private void ensureSameHeaderSize(List<String> expected, List<String> actual, String sourceFile) {
        if (actual.size() != expected.size()) {
            throw new CsvFileFormatException(
                    sourceFile,
                    "CSV header column count mismatch. expected=" + expected.size()
                    + " actual=" + actual.size()
                    + " expectedHeader=" + expected
                    + " actualHeader=" + actual
            );
        }
    }

    private void ensureSameHeaderNames(List<String> expected, List<String> actual, String sourceFile) {
        for (int i = 0; i < expected.size(); i++) {
            String exp = normalize(expected.get(i));
            String act = normalize(actual.get(i));

            if (!exp.equalsIgnoreCase(act)) {
                throw new CsvFileFormatException(
                        sourceFile,
                        "CSV header mismatch at index " + i
                        + ". expected='" + expected.get(i)
                        + "' actual='" + actual.get(i) + "'"
                );
            }
        }
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    private static void closeQuietly(CSVParser parser) {
        try {
            parser.close();
        } catch (IOException ignored) {
        }
    }

    private static final class CommonsCursor implements CsvRowCursor {

        private final Path csvFile;
        private final CSVParser parser;
        private final Iterator<CSVRecord> iterator;
        private final int expectedColumns;
        private final char delimiter;
        private final String sourceFile;

        private boolean closed;

        private CommonsCursor(
                Path csvFile,
                CSVParser parser,
                Iterator<CSVRecord> iterator,
                int expectedColumns,
                char delimiter,
                String sourceFile
        ) {
            this.csvFile = csvFile;
            this.parser = parser;
            this.iterator = iterator;
            this.expectedColumns = expectedColumns;
            this.delimiter = delimiter;
            this.sourceFile = sourceFile;
        }

        @Override
        public boolean hasNext() {
            try {
                boolean has = iterator.hasNext();
                if (!has) {
                    close();
                }
                return has;
            } catch (RuntimeException e) {
                close();
                throw new CsvFileReadException(sourceFile, "CSV read failed while iterating records", e);
            }
        }

        @Override
        public RowData next() {
            try {
                CSVRecord record = iterator.next();

                ensureExpectedColumnCount(record);

                List<String> values = record.stream().collect(Collectors.toList());
                String reconstructedRow = join(values, delimiter);

                return new RowData(
                        csvFile.toAbsolutePath().normalize().toString(),
                        record.getRecordNumber(),
                        values,
                        reconstructedRow
                );

            } catch (CsvRowReadException e) {
                throw e;

            } catch (RuntimeException e) {
                close();
                throw new CsvFileReadException(sourceFile, "CSV read failed while reading a record", e);
            }
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            closeQuietly(parser);
        }

        private void ensureExpectedColumnCount(CSVRecord record) {
            int actual = record.size();
            if (actual != expectedColumns) {
                List<String> values = record.stream().collect(Collectors.toList());
                String reconstructedRow = join(values, delimiter);

                throw new CsvRowReadException(
                        sourceFile,
                        record.getRecordNumber(),
                        reconstructedRow,
                        "CSV column count mismatch. expected=" + expectedColumns + " actual=" + actual
                );
            }
        }

        private static String join(List<String> values, char delimiter) {
            String d = String.valueOf(delimiter);
            return values.stream()
                    .map(v -> v == null ? "" : v)
                    .collect(Collectors.joining(d));
        }
    }
}
