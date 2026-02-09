package io.github.mobinrt.csvparser.domain.validation;

import io.github.mobinrt.csvparser.domain.model.RowData;

public interface RowTransformer {

    RowTransformResult transform(RowData row);
}
