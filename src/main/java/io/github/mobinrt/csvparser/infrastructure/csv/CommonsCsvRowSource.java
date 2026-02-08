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

import io.github.mobinrt.csvparser.domain.model.ColumnDef;
import io.github.mobinrt.csvparser.domain.model.RowData;
import io.github.mobinrt.csvparser.domain.model.Schema;
import io.github.mobinrt.csvparser.domain.ports.CsvRowCursor;
import io.github.mobinrt.csvparser.domain.ports.CsvRowSource;

public final class CommonsCsvRowSource implements CsvRowSource {

    @Override
    public CsvRowCursor openCursor(Path csvFile, Schema schema) {
        Objects.requireNonNull(csvFile, "csvFile");
        Objects.requireNonNull(schema, "schema");

        try {
            BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(schema.getCsv().getDelimiter())
                    .setQuote(schema.getCsv().getQuote())
                    .setTrim(true)
                    .setIgnoreSurroundingSpaces(true)
                    .setIgnoreEmptyLines(true)
                    .setAllowMissingColumnNames(false)
                    .setLenientEof(true)
                    .setHeader(schema.getCsv().hasHeader() ? new String[0] : null)
                    .setSkipHeaderRecord(schema.getCsv().hasHeader())
                    .build();

            CSVParser parser = new CSVParser(reader, format);

            if (schema.getCsv().hasHeader()) {
                validateHeader(parser, schema, csvFile);
            }

            Iterator<CSVRecord> it = parser.iterator();
            return new CommonsCursor(csvFile, schema, parser, it);

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to open CSV file: " + csvFile, e);
        }
    }

    private void validateHeader(CSVParser parser, Schema schema, Path csvFile) {
        List<String> actual = parser.getHeaderNames();
        List<String> expected = schema.getColumns().stream()
                .map(ColumnDef::getName)
                .toList();

        if (actual.size() != expected.size()) {
            throw new IllegalArgumentException(
                    "CSV header column count mismatch in " + csvFile
                    + ". expected=" + expected.size() + " actual=" + actual.size()
                    + " expectedHeader=" + expected + " actualHeader=" + actual
            );
        }

        for (int i = 0; i < expected.size(); i++) {
            String exp = normalizeHeader(expected.get(i));
            String act = normalizeHeader(actual.get(i));
            if (!exp.equalsIgnoreCase(act)) {
                throw new IllegalArgumentException(
                        "CSV header mismatch at index " + i + " in " + csvFile
                        + ". expected='" + expected.get(i) + "' actual='" + actual.get(i) + "'"
                );
            }
        }
    }

    private String normalizeHeader(String s) {
        return s == null ? "" : s.trim();
    }

    private static final class CommonsCursor implements CsvRowCursor {

        private final Path csvFile;
        private final Schema schema;
        private final CSVParser parser;
        private final Iterator<CSVRecord> iterator;
        private boolean closed = false;

        private CommonsCursor(Path csvFile, Schema schema, CSVParser parser, Iterator<CSVRecord> iterator) {
            this.csvFile = csvFile;
            this.schema = schema;
            this.parser = parser;
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            boolean has = iterator.hasNext();
            if (!has) {
                close();
            }
            return has;
        }

        @Override
        public RowData next() {
            CSVRecord record = iterator.next();

            int expectedCols = schema.getColumns().size();
            if (record.size() != expectedCols) {
                String raw = buildRawRow(record);
                throw new IllegalArgumentException(
                        "CSV column count mismatch in " + csvFile
                        + " at row " + record.getRecordNumber()
                        + ". expected=" + expectedCols + " actual=" + record.size()
                        + " rawRow=" + raw
                );
            }

            List<String> values = toValues(record);
            String rawRow = buildRawRow(values, schema.getCsv().getDelimiter());

            return new RowData(
                    csvFile.toAbsolutePath().normalize().toString(),
                    record.getRecordNumber(),
                    values,
                    rawRow
            );
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                parser.close();
            } catch (IOException ignored) {
            }
        }

        private List<String> toValues(CSVRecord record) {
            return record.stream().collect(Collectors.toList());
        }

        private String buildRawRow(CSVRecord record) {
            List<String> values = record.stream().collect(Collectors.toList());
            return buildRawRow(values, schema.getCsv().getDelimiter());
        }

        private String buildRawRow(List<String> values, char delimiter) {
            String d = String.valueOf(delimiter);
            return values.stream()
                    .map(v -> v == null ? "" : v)
                    .collect(Collectors.joining(d));
        }
    }
}
