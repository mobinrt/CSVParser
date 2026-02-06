package io.github.mobinrt.csvparser.domain.ports;

import java.nio.file.Path;
import java.util.List;

import io.github.mobinrt.csvparser.domain.model.ScanMode;

public interface InputResolver {

    /**
     * Resolve a deterministic list of CSV files from input paths.
     *
     * @param inputs file and/or folder paths
     * @param recursive whether to scan folders recursively
     * @return sorted list of CSV file paths
     */
    List<Path> resolveCsvFiles(List<Path> inputs, ScanMode scanMode);
}
