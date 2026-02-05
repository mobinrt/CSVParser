package io.github.mobinrt.csvparser.usecase;

import java.nio.file.Path;
import java.util.List;

public final class ParseRequest {

    private final Path schemaPath;
    private final List<Path> inputs;
    private final boolean recursive;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPass;
    private final String tableOverride;
    private final List<String> includeColumns;
    private final int batchSize;
    private final boolean validateTypes;
    private final boolean stopOnFatal;
    private final long maxErrors;

    private ParseRequest(Builder b) {
        this.schemaPath = b.schemaPath;
        this.inputs = List.copyOf(b.inputs);
        this.recursive = b.recursive;
        this.dbUrl = b.dbUrl;
        this.dbUser = b.dbUser;
        this.dbPass = b.dbPass;
        this.tableOverride = b.tableOverride;
        this.includeColumns = List.copyOf(b.includeColumns);
        this.batchSize = b.batchSize;
        this.validateTypes = b.validateTypes;
        this.stopOnFatal = b.stopOnFatal;
        this.maxErrors = b.maxErrors;
    }

    public Path schemaPath() {
        return schemaPath;
    }

    public List<Path> inputs() {
        return inputs;
    }

    public boolean recursive() {
        return recursive;
    }

    public String dbUrl() {
        return dbUrl;
    }

    public String dbUser() {
        return dbUser;
    }

    public String dbPass() {
        return dbPass;
    }

    public String tableOverride() {
        return tableOverride;
    }

    public List<String> includeColumns() {
        return includeColumns;
    }

    public int batchSize() {
        return batchSize;
    }

    public boolean validateTypes() {
        return validateTypes;
    }

    public boolean stopOnFatal() {
        return stopOnFatal;
    }

    public long maxErrors() {
        return maxErrors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Path schemaPath;
        private List<Path> inputs;
        private boolean recursive;
        private String dbUrl;
        private String dbUser;
        private String dbPass;
        private String tableOverride;
        private List<String> includeColumns;
        private int batchSize;
        private boolean validateTypes;
        private boolean stopOnFatal;
        private long maxErrors;

        public Builder schemaPath(Path v) {
            this.schemaPath = v;
            return this;
        }

        public Builder inputs(List<Path> v) {
            this.inputs = v;
            return this;
        }

        public Builder recursive(boolean v) {
            this.recursive = v;
            return this;
        }

        public Builder dbUrl(String v) {
            this.dbUrl = v;
            return this;
        }

        public Builder dbUser(String v) {
            this.dbUser = v;
            return this;
        }

        public Builder dbPass(String v) {
            this.dbPass = v;
            return this;
        }

        public Builder tableOverride(String v) {
            this.tableOverride = v;
            return this;
        }

        public Builder includeColumns(List<String> v) {
            this.includeColumns = v;
            return this;
        }

        public Builder batchSize(int v) {
            this.batchSize = v;
            return this;
        }

        public Builder validateTypes(boolean v) {
            this.validateTypes = v;
            return this;
        }

        public Builder stopOnFatal(boolean v) {
            this.stopOnFatal = v;
            return this;
        }

        public Builder maxErrors(long v) {
            this.maxErrors = v;
            return this;
        }

        public ParseRequest build() {
            if (schemaPath == null) {
                throw new IllegalArgumentException("--schema is required");
            }
            if (inputs == null || inputs.isEmpty()) {
                throw new IllegalArgumentException("At least one --input is required");
            }
            return new ParseRequest(this);
        }
    }
}
