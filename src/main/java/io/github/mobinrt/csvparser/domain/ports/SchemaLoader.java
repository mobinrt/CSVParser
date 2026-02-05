package io.github.mobinrt.csvparser.domain.ports;

import java.nio.file.Path;

import io.github.mobinrt.csvparser.domain.model.Schema;

public interface SchemaLoader {

    Schema load(Path schemaPath);
}
