package io.github.mobinrt.csvparser.domain.model;

import java.util.List;
import java.util.Objects;

public final class ColumnSelection {

    private final List<ColumnDef> selectedColumns;
    private final int[] selectedIndices;

    public ColumnSelection(List<ColumnDef> selectedColumns, int[] selectedIndices) {
        this.selectedColumns = List.copyOf(Objects.requireNonNull(selectedColumns, "selectedColumns"));
        this.selectedIndices = Objects.requireNonNull(selectedIndices, "selectedIndices").clone();
    }

    public List<ColumnDef> getSelectedColumns() {
        return selectedColumns;
    }

    public int[] getSelectedIndices() {
        return selectedIndices.clone();
    }
}
