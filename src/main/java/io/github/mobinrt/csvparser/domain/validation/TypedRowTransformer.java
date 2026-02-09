package io.github.mobinrt.csvparser.domain.validation;

import java.util.List;

import io.github.mobinrt.csvparser.domain.model.ColumnDef;
import io.github.mobinrt.csvparser.domain.model.RowData;

public final class TypedRowTransformer implements RowTransformer {

    private final List<ColumnDef> selectedColumns;
    private final int[] selectedIndices;
    private final ValueConverter valueConverter = new ValueConverter();

    public TypedRowTransformer(List<ColumnDef> selectedColumns, int[] selectedIndices) {
        this.selectedColumns = List.copyOf(selectedColumns);
        this.selectedIndices = selectedIndices.clone();
    }

    @Override
    public RowTransformResult transform(RowData row) {
        try {
            List<String> values = row.getValues();
            Object[] out = new Object[selectedIndices.length];

            for (int i = 0; i < selectedIndices.length; i++) {
                ColumnDef col = selectedColumns.get(i);
                String raw = values.get(selectedIndices[i]);
                out[i] = valueConverter.coerce(col.getType(), raw);
            }
            return RowTransformResult.ok(out);

        } catch (IllegalArgumentException ex) {
            return RowTransformResult.error(ex.getMessage());
        }
    }
}
