package io.github.mobinrt.csvparser.domain.services;

public enum ScanMode {
    NON_RECURSIVE,
    RECURSIVE;

    public boolean isRecursive() {
        return this == RECURSIVE;
    }
}
