package io.github.mobinrt.csvparser.domain.ports;

import io.github.mobinrt.csvparser.domain.model.ErrorRow;

public interface ErrorWriter {

    void ensureErrorTableExist();

    void writeErrorRow(ErrorRow errorRow);
}
