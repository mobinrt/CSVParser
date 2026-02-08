package io.github.mobinrt.csvparser.domain.ports;

import java.nio.file.Path;

import io.github.mobinrt.csvparser.domain.model.Schema;

public interface CsvRowSource {

    CsvRowCursor openCursor(Path csvFile, Schema schema);
}
