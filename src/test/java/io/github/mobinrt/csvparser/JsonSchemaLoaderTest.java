package io.github.mobinrt.csvparser;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import io.github.mobinrt.csvparser.domain.model.Schema;
import io.github.mobinrt.csvparser.infrastructure.schema.JsonSchemaLoader;

class JsonSchemaLoaderTest {

    @Test
    void loadsSchemaFromJsonFile() throws Exception {
        String json = """
                {
                  "tableName": "import_data",
                  "columns": [
                    { "name": "id", "type": "INT" },
                    { "name": "name", "type": "VARCHAR(255)" }
                  ],
                  "csv": { "delimiter": ",", "quote": "\\"",
                           "hasHeader": true }
                }
                """;

        Path temp = Files.createTempFile("schema", ".json");
        Files.writeString(temp, json);

        Schema schema = new JsonSchemaLoader().load(temp);

        assertEquals("import_data", schema.getTableName());
        assertEquals(2, schema.getColumns().size());
        assertEquals(',', schema.getCsv().getDelimiter());
        assertEquals('"', schema.getCsv().getQuote());
        assertTrue(schema.getCsv().hasHeader());
    }
}
