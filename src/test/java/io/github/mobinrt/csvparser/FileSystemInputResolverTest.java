package io.github.mobinrt.csvparser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import io.github.mobinrt.csvparser.domain.model.ScanMode;
import io.github.mobinrt.csvparser.infrastructure.filesystem.FileSystemInputResolver;

class FileSystemInputResolverTest {

    @Test
    void resolvesCsvFilesFromFolder_nonRecursive() throws Exception {
        Path dir = Files.createTempDirectory("inputs");
        Files.writeString(dir.resolve("a.csv"), "x\n");
        Files.writeString(dir.resolve("b.CSV"), "x\n");
        Files.writeString(dir.resolve("c.txt"), "x\n");

        FileSystemInputResolver resolver = new FileSystemInputResolver();
        List<Path> out = resolver.resolveCsvFiles(List.of(dir), ScanMode.NON_RECURSIVE);

        assertEquals(2, out.size());
        assertTrue(out.get(0).getFileName().toString().toLowerCase().endsWith(".csv"));
        assertTrue(out.get(1).getFileName().toString().toLowerCase().endsWith(".csv"));
    }

    @Test
    void resolvesCsvFilesFromFolder_recursive() throws Exception {
        Path dir = Files.createTempDirectory("inputs");
        Path sub = Files.createDirectory(dir.resolve("sub"));
        Files.writeString(sub.resolve("x.csv"), "x\n");
        Files.writeString(dir.resolve("root.csv"), "x\n");

        FileSystemInputResolver resolver = new FileSystemInputResolver();
        List<Path> out = resolver.resolveCsvFiles(List.of(dir), ScanMode.RECURSIVE);

        assertEquals(2, out.size());
    }

    @Test
    void resolvesCsvFilesFromMixedInputs() throws Exception {
        Path dir = Files.createTempDirectory("inputs");
        Path file = Files.createTempFile("single", ".csv");
        Files.writeString(dir.resolve("a.csv"), "x\n");

        FileSystemInputResolver resolver = new FileSystemInputResolver();
        List<Path> out = resolver.resolveCsvFiles(List.of(dir, file), ScanMode.NON_RECURSIVE);

        assertEquals(2, out.size());
    }

    @Test
    void throwsWhenInputDoesNotExist() {
        Path missing = Path.of("this-path-should-not-exist-xyz");

        FileSystemInputResolver resolver = new FileSystemInputResolver();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolveCsvFiles(List.of(missing), ScanMode.NON_RECURSIVE)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("does not exist"));
    }
}
