package io.github.mobinrt.csvparser.domain.validation;

import java.util.List;

import io.github.mobinrt.csvparser.domain.model.RowData;

public final class NoOpRowTransformer implements RowTransformer {

    private final int[] selectedIndices;

    public NoOpRowTransformer(int[] selectedIndices) {
        this.selectedIndices = selectedIndices.clone();
    }

    @Override
    public RowTransformResult transform(RowData row) {
        List<String> values = row.getValues();
        Object[] out = new Object[selectedIndices.length];

        for (int i = 0; i < selectedIndices.length; i++) {
            String v = values.get(selectedIndices[i]);
            out[i] = normalizeEmptyToNull(v);
        }

        return RowTransformResult.ok(out);
    }

    private Object normalizeEmptyToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
