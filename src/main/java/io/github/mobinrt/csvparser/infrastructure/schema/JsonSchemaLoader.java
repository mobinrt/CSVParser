package io.github.mobinrt.csvparser.infrastructure.schema;


import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.mobinrt.csvparser.domain.model.ColumnDef;
import io.github.mobinrt.csvparser.domain.model.CsvFormat;
import io.github.mobinrt.csvparser.domain.model.Schema;
import io.github.mobinrt.csvparser.domain.ports.SchemaLoader;

public final class JsonSchemaLoader implements SchemaLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Schema load(Path schemaPath) {
        try {
            RawSchema raw = mapper.readValue(schemaPath.toFile(), RawSchema.class);

            CsvFormat csv = new CsvFormat(
                    raw.csv.delimiter.charAt(0),
                    raw.csv.quote.charAt(0),
                    raw.csv.hasHeader
            );

            List<ColumnDef> columns = raw.columns.stream()
                    .map(c -> new ColumnDef(c.name, c.type))
                    .toList();

            return new Schema(raw.tableName, columns, csv);

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read schema JSON: " + schemaPath, e);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid schema JSON structure: " + schemaPath + " (" + e.getMessage() + ")", e);
        }
    }

    static final class RawSchema {
        public String tableName;
        public List<RawColumn> columns;
        public RawCsv csv;
    }

    static final class RawColumn {
        public String name;
        public String type;
    }

    static final class RawCsv {
        public String delimiter = ",";
        public String quote = "\"";
        public boolean hasHeader = true;
    }
}
