package io.github.mobinrt.csvparser.infrastructure.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mobinrt.csvparser.domain.model.ScanMode;
import io.github.mobinrt.csvparser.domain.ports.InputResolver;

public final class FileSystemInputResolver implements InputResolver {

    private static final Logger log = LoggerFactory.getLogger(FileSystemInputResolver.class);

    @Override
    public List<Path> resolveCsvFiles(List<Path> inputs, ScanMode scanMode) {
        validateInputs(inputs);

        List<Path> results = new ArrayList<>();

        for (Path input : inputs) {
            if (input == null) {
                continue;
            }
            Path normalized = normalizePath(input);
            addCsvFilesFromInput(results, normalized, scanMode);
        }

        return sortAndFreezeResults(results);
    }

    private void validateInputs(List<Path> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("inputs must not be empty");
        }
    }

    private Path normalizePath(Path input) {
        return input.toAbsolutePath().normalize();
    }

    private void addCsvFilesFromInput(List<Path> results, Path input, ScanMode scanMode) {
        ensurePathExists(input);

        if (isRegularFile(input)) {
            addCsvFileIfApplicable(results, input);
            return;
        }

        if (isDirectory(input)) {
            results.addAll(scanDirectoryForCsvFiles(input, scanMode));
            return;
        }

        log.warn("Skipping unsupported path type: {}", input);
    }

    private void ensurePathExists(Path input) {
        if (!Files.exists(input)) {
            throw new IllegalArgumentException("Input path does not exist: " + input);
        }
    }

    private boolean isRegularFile(Path input) {
        return Files.isRegularFile(input);
    }

    private boolean isDirectory(Path input) {
        return Files.isDirectory(input);
    }

    private void addCsvFileIfApplicable(List<Path> results, Path file) {
        if (isCsvFile(file)) {
            results.add(file);
            return;
        }
        log.warn("Skipping non-CSV file input: {}", file);
    }

    private List<Path> scanDirectoryForCsvFiles(Path directory, ScanMode scanMode) {
        try (Stream<Path> stream = openDirectoryStream(directory, scanMode)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isCsvFile)
                    .map(this::normalizePath)
                    .toList();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to scan directory: " + directory, e);
        }
    }

    private Stream<Path> openDirectoryStream(Path directory, ScanMode scanMode) throws IOException {
        return scanMode.isRecursive() ? Files.walk(directory) : Files.list(directory);
    }

    private boolean isCsvFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".csv");
    }

    private List<Path> sortAndFreezeResults(List<Path> results) {
        results.sort(Comparator.comparing(Path::toString));
        return List.copyOf(results);
    }
}
