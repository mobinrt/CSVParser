package io.github.mobinrt.csvparser.domain.ports;

import java.io.Closeable;
import java.util.Iterator;

import io.github.mobinrt.csvparser.domain.model.RowData;

/**
 * Streams rows from a CSV file. Must be closed to release file handles.
 */
public interface CsvRowCursor extends Iterator<RowData>, Closeable {

    @Override
    void close();
}
