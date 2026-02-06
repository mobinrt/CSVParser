package io.github.mobinrt.csvparser.domain.ports;

import java.util.List;

import io.github.mobinrt.csvparser.domain.model.Schema;

public interface TableWriter {

    /**
     * Create the data table (IF NOT EXISTS) using schema + selected columns. if
     * includeColumns is empty/null => all schema columns.
     */
    void ensureDataTableExists(Schema schema, String tableOverride, List<String> includeColumns);
}
