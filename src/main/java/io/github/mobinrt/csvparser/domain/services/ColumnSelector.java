package io.github.mobinrt.csvparser.domain.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.github.mobinrt.csvparser.domain.model.ColumnDef;
import io.github.mobinrt.csvparser.domain.model.ColumnSelection;
import io.github.mobinrt.csvparser.domain.model.Schema;

public class ColumnSelector {

    private ColumnSelector() {
    }

    public static ColumnSelection selectColumns(Schema schema, List<String> includeColumns) {
        Objects.requireNonNull(schema, "schema must not be null.");

        List<ColumnDef> allColumns = schema.getColumns();

        if (includeColumns == null || includeColumns.isEmpty()) {
            return selectAll(allColumns);
        }

        return selectIncluded(allColumns, includeColumns);

    }

    private static ColumnSelection selectAll(List<ColumnDef> allColumns) {
        int[] indices = new int[allColumns.size()];
        for (int i = 0; i < allColumns.size(); i++) {
            indices[i] = i;
        }
        return new ColumnSelection(allColumns, indices);
    }

    private static ColumnSelection selectIncluded(List<ColumnDef> all, List<String> includeColumns) {
        Map<String, Integer> indexByLower = new HashMap<>();
        for (int i = 0; i < all.size(); i++) {
            String name = all.get(i).getName();
            indexByLower.put(name.toLowerCase(Locale.ROOT), i);
        }

        List<ColumnDef> selected = new ArrayList<>();
        int[] indices = new int[includeColumns.size()];
        int pos = 0;

        for (String name : includeColumns) {
            if (name == null || name.isBlank()) {
                continue;
            }

            String key = name.trim().toLowerCase(Locale.ROOT);
            Integer idx = indexByLower.get(key);

            if (idx == null) {
                throw new IllegalArgumentException("include-columns contains unknown column: " + name);
            }

            selected.add(all.get(idx));
            indices[pos++] = idx;
        }

        if (selected.isEmpty()) {
            throw new IllegalArgumentException("include-columns resolved to empty selection");
        }

        if (pos < indices.length) {
            indices = Arrays.copyOf(indices, pos);
        }

        return new ColumnSelection(selected, indices);
    }

}
